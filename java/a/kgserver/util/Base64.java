package a.kgserver.util;

import javax.xml.bind.DatatypeConverter;

public class Base64
{
  public static String encode( String src ) throws Exception
  {
    return DatatypeConverter.printBase64Binary(
             src.getBytes(DEFAULT_ENCODING) );
  }

  public static String encode( byte[] bytes ) throws Exception
  {
    return DatatypeConverter.printBase64Binary(bytes);
  }

  public static byte[] decode( String src ) throws Exception
  {
    return DatatypeConverter.parseBase64Binary( src );
  }

  public static final String DEFAULT_ENCODING = "UTF-8";
}
