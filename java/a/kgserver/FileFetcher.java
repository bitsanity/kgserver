package a.kgserver;

import java.nio.file.*;

// iResourceFetcher implementation that locates resources within
// the $CWD/resources/ folder

public class FileFetcher implements iResourceFetcher
{
  public FileFetcher() {}

  public byte[] fetch( String resource )
  {
    try {
      Path path = FileSystems.getDefault().getPath("resources", resource );
      return Files.readAllBytes( path );
    }
    catch( Exception e ) {
      e.printStackTrace();
    }

    return null;
  }
}

