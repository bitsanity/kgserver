package a.kgserver;

// Implementations to determine how to map a simple string from a
// request to its named resource

public interface iResourceFetcher
{
  // returns contents of specified resource, or null if not found
  public byte[] fetch( String res );
}
