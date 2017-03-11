package a.kgserver;

import java.io.*;
import java.net.*;
import java.util.Arrays;

import org.json.simple.*;
import org.json.simple.parser.*;

import a.kgserver.util.*;

public class KGWorker extends Thread
{
  private static final int ERR_CHALL = 1;
  private static final int ERR_NORES = 2;
  private static final int ERR_BAD_G = 3;
  private static final int ERR_K_UNK = 4;

  private static final iResourceFetcher fetcher_ = new FileFetcher();

  private static ACL acl_ = null;

  private Socket client_ = null;
  private boolean testmode_ = false;

  private Challenge ch_ = null;  // contains pub (G) and priv (g) keys
  private byte[] A_ = null;      // agent acting on client's behalf
  private byte[] K_ = null;      // client's public key
  private String id_ = null;     // client.cookie

  private KGWorker() {}

  public KGWorker( Socket client, boolean testmode ) throws Exception
  {
    acl_ = new ACL();
    client_ = client;
    testmode_ = testmode;
  }

  public void run()
  {
    try {
      work();
    }
    catch( Exception e ) {
      e.printStackTrace();
    }

    try {
      client_.close();
    }
    catch( Exception e ) { }
  }

  // process requests and return responses until the connection
  // is closed (exception will be thrown)

  private void work() throws Exception
  {
    BufferedReader rdr = new BufferedReader(
      new InputStreamReader(client_.getInputStream()) );

    PrintWriter pw = new PrintWriter( client_.getOutputStream(), true );

    JSONParser parser = null; // different parser for each interaction

    while (true)
    {
      String msg = rdr.readLine();

      if (null == msg) break;

      parser = new JSONParser();
      JSONObject jreq = (JSONObject) parser.parse( msg );

      JSONObject repl = replyTo( jreq );

      pw.println( repl.toJSONString() );
    }
  }

  // test the request and dispatch accordingly
  private JSONObject replyTo( JSONObject request ) throws Exception
  {
    String method = (String) request.get( "method" );
    JSONArray params = (JSONArray) request.get( "params" );

    if (testmode_ && method.equals( "test.register" ))
    {
      String b64key = (String) params.get( 0 );
      return register( b64key );
    }

    if (method.equals( "adilos.response" ))
    {
      String adilosResp = (String) params.get( 0 );

      Message rspmsg = Message.parse( adilosResp );

      byte[] G = rspmsg.part( 0 ).key();
      byte[] A = rspmsg.part( 1 ).key();
      byte[] K = rspmsg.part( 2 ).key();

      if (!Arrays.equals(ch_.pubKey(), G))
        return errorMessage( ERR_BAD_G, "Invalid G", HexString.encode(G), id_ );

      if ( !acl_.hasKey(K) )
        return errorMessage( ERR_K_UNK, "K Unknown", HexString.encode(K), id_ );

      A_ = A;
      K_ = K;

      return defaultPage();
    }

    id_ = (String) request.get( "id" );

    if (method.equals( "request" ))
      return handleRequest( request );

    // if arrived here then request is unrecognized so challenge
    return challenge();
  }

  private JSONObject challenge() throws Exception
  {
    ch_ = new Challenge();

    return errorMessage( ERR_CHALL, "adilos.challenge", ch_.toString(), null );
  }

  private JSONObject errorMessage( int code,
                                   String message,
                                   String data,
                                   String id ) throws Exception
  {
    JSONObject errbody = new JSONObject();
    errbody.put( "code", new Integer(code) );
    errbody.put( "message", (null != message ? message : "null") );
    errbody.put( "data", (null != data ? data : "null") );

    JSONObject errmsg = new JSONObject();
    errmsg.put( "result", "null" );
    errmsg.put( "error", errbody );
    errmsg.put( "id", (null != id ? id : "null") );
    return errmsg;
  }

  // reply with contents of $CWD/resources/default
  private JSONObject defaultPage() throws Exception
  {
    byte[] redbytes = fetcher_.fetch( "default" );

    Secp256k1 curve = new Secp256k1();
    ECIES ec = new ECIES( ch_.privKey(), A_ );
    String rspb64 = ec.encrypt( redbytes );
    byte[] sig =
      curve.signECDSA( SHA256.hash(rspb64.getBytes()), ch_.privKey() );

    JSONObject rsp = new JSONObject();
    rsp.put( "rsp", rspb64 );
    rsp.put( "sig", Base64.encode(sig) );

    JSONObject reply = new JSONObject();
    reply.put( "result", rsp );
    reply.put( "error", "null" );
    reply.put( "id", ((null != id_) ? id_ : "null" ) );

    return reply;
  }

  private JSONObject handleRequest( JSONObject request ) throws Exception
  {
    JSONArray arr = (JSONArray) request.get( "params" );
    if (null == arr || 1 != arr.size()) return challenge();

    JSONObject blob = (JSONObject) arr.get( 0 );

    String req64 = (String) blob.get("req");
    byte[] sigA = Base64.decode( (String) blob.get("sig") );

    // confirm A signed the request correctly
    Secp256k1 curve = new Secp256k1();

    if (!curve.verifyECDSA(sigA, SHA256.hash(req64.getBytes()), A_))
    {
      System.out.println( "request: sig failed" );
      return challenge();
    }

    // decrypt request
    ECIES ec = new ECIES( ch_.privKey(), A_ );
    byte[] req = ec.decrypt( req64 );

    // fetch named resource
    String reqS = new String( req, "UTF-8" );

    byte[] rawrsp = fetcher_.fetch( reqS );

    if (null == rawrsp)
      return errorMessage( ERR_NORES, "Not found", reqS , id_ );

    // encrypt then sign the encrypted version
    String rsp64 = ec.encrypt( rawrsp );
    byte[] sigG =
      curve.signECDSA( SHA256.hash(rsp64.getBytes()), ch_.privKey() );

    JSONObject rsp = new JSONObject();
    rsp.put( "rsp", rsp64 );
    rsp.put( "sig", Base64.encode(sigG) );

    JSONObject reply = new JSONObject();
    reply.put( "result", rsp );
    reply.put( "error", "null" );
    reply.put( "id", ((null != id_) ? id_ : "null" ) );

    return reply;
  }

  private JSONObject register( String b64Key ) throws Exception
  {
    acl_.addKey( Base64.decode(b64Key) );
    return challenge(); // still have to authenticate
  }

  //
  // Test code
  //
  public static void main( String[] args ) throws Exception
  {
    KGWorker wkr = new KGWorker( null, true );

    Secp256k1 curve = new Secp256k1();

    // setup test identities for keymaster and agent

    byte[] a = new byte[32];
    java.security.SecureRandom.getInstance("SHA1PRNG").nextBytes( a );
    byte[] A = curve.publicKeyCreate( a );

    byte[] k = new byte[32];
    java.security.SecureRandom.getInstance("SHA1PRNG").nextBytes( k );
    byte[] K = curve.publicKeyCreate( k );

    wkr.register( Base64.encode(K) );

    //
    // prompt worker to generate a challenge
    //

    JSONObject blankRequest = new JSONObject();
    JSONArray blankArray = new JSONArray();
    blankRequest.put( "method", "request" );
    blankRequest.put( "params", blankArray );
    blankRequest.put( "id", "null" );

    JSONObject challO = wkr.replyTo( blankRequest );

    JSONObject err = (JSONObject) challO.get( "error" );

    String data = (String) err.get( "data" );

    Message chmsg = Message.parse( data );

    //
    // generate the response
    //

    MessagePart gpart = new MessagePart( chmsg.part(0).key(),
                                         chmsg.part(0).sig() );

    byte[] asig = curve.signECDSA( SHA256.hash(gpart.sig()), a );
    MessagePart apart = new MessagePart( A, asig );

    // pretend to exchange QRs with keymaster who provides ksig and K
    byte[] ksig = curve.signECDSA( SHA256.hash(asig), k );
    MessagePart kpart = new MessagePart( K, ksig );

    Message armsg = new Message( new MessagePart[] { gpart, apart, kpart } );

    JSONObject adilosRsp = new JSONObject();
    adilosRsp.put( "method", "adilos.response" );

    JSONArray params = new JSONArray();
    params.add( armsg.toString() );

    adilosRsp.put( "params", params );
    adilosRsp.put( "id", "null" );

    //
    // Processes response and returns the default page
    //
    JSONObject dflt = wkr.replyTo( adilosRsp );

    System.out.println( "PASS" );
  }

}
