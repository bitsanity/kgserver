package a.kgserver;

import java.io.*;
import java.net.*;
import java.security.*;
import java.sql.*;

import a.kgserver.util.*;

public class KGServer
{
  public KGServer( int port, boolean testmode ) throws Exception
  {
    boolean keepGoing = true;

    ServerSocket ss = new ServerSocket( port );

    while( keepGoing )
      new KGWorker( ss.accept(), testmode ).start();

    ss.close();
  }

  public static void main( String[] args ) throws Exception
  {
    if (2 != args.length)
    {
      System.out.println( "KGServer <port> test=0|1" );
      return;
    }

    boolean testmode =
        ( 0 == Integer.parseInt(args[1].split("=")[1]) );

    KGServer kgs = new KGServer( Integer.parseInt(args[0]), testmode );
  }

}
