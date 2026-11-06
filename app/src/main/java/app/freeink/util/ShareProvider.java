package app.freeink.util;
import android.content.*; import android.database.*; import android.net.Uri; import android.os.ParcelFileDescriptor; import java.io.File;

/** Minimal FileProvider (framework only) serving files from cacheDir/shared. */
public class ShareProvider extends ContentProvider {
    public boolean onCreate(){ return true; }
    private File file(Uri u){ return new File(new File(getContext().getCacheDir(),"shared"),u.getLastPathSegment()); }
    public ParcelFileDescriptor openFile(Uri u,String m) throws java.io.FileNotFoundException { return ParcelFileDescriptor.open(file(u),ParcelFileDescriptor.MODE_READ_ONLY); }
    public String getType(Uri u){ String n=u.getLastPathSegment(); return n.endsWith(".pdf")?"application/pdf":n.endsWith(".png")?"image/png":"text/plain"; }
    public Cursor query(Uri u,String[]p,String s,String[]a,String o){ return null; }
    public Uri insert(Uri u,ContentValues v){ return null; } public int delete(Uri u,String s,String[]a){ return 0; } public int update(Uri u,ContentValues v,String s,String[]a){ return 0; }
    public static Uri uriFor(Context c,File f){ new File(c.getCacheDir(),"shared").mkdirs(); File t=new File(new File(c.getCacheDir(),"shared"),f.getName());
        try{ DocStoreCopy.copy(f,t); }catch(Exception ignored){} return Uri.parse("content://app.freeink.files/"+f.getName()); }
    static class DocStoreCopy{ static void copy(File a,File b) throws Exception{ try(java.io.FileInputStream in=new java.io.FileInputStream(a); java.io.FileOutputStream o=new java.io.FileOutputStream(b)){ byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0)o.write(buf,0,n);} } }
}
