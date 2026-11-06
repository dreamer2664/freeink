package app.freeink.data;
import android.content.*; import android.net.Uri; import org.json.*;
import java.io.*; import java.util.*;

public class DocStore {
    public static class Doc { public String id,title,type,folder="All",snippet=""; public long updated; public boolean favorite; public int color=0xFF0071E3,pdfPages; public String pdfName="";
        JSONObject toJson() throws JSONException { JSONObject o=new JSONObject(); o.put("id",id);o.put("title",title);o.put("type",type);o.put("folder",folder);o.put("updated",updated);o.put("fav",favorite);o.put("color",color);o.put("pdfPages",pdfPages);o.put("pdfName",pdfName);o.put("snippet",snippet); return o; }
        static Doc fromJson(JSONObject o){ Doc d=new Doc(); d.id=o.optString("id");d.title=o.optString("title","Untitled");d.type=o.optString("type","note");d.folder=o.optString("folder","All");d.updated=o.optLong("updated");d.favorite=o.optBoolean("fav");d.color=o.optInt("color",0xFF0071E3);d.pdfPages=o.optInt("pdfPages");d.pdfName=o.optString("pdfName");d.snippet=o.optString("snippet"); return d; } }
    private final File root, indexFile; private final List<Doc> docs=new ArrayList<>(); private final List<String> folders=new ArrayList<>();
    public DocStore(Context c){ root=new File(c.getFilesDir(),"freeink"); new File(root,"docs").mkdirs(); new File(root,"media").mkdirs(); new File(root,"pdf").mkdirs(); indexFile=new File(root,"index.json"); load(); }
    public List<Doc> docs(){ return docs; } public List<String> folders(){ if(!folders.contains("All"))folders.add(0,"All"); return folders; }
    public File docFile(String id){ return new File(new File(root,"docs"),id+".json"); }
    public File mediaDir(){ return new File(root,"media"); } public File pdfFile(String id){ return new File(new File(root,"pdf"),id+".pdf"); }
    private void load(){ try{ if(indexFile.exists()){ JSONObject o=new JSONObject(read(indexFile)); JSONArray a=o.optJSONArray("docs"); if(a!=null)for(int i=0;i<a.length();i++)docs.add(Doc.fromJson(a.getJSONObject(i)));
        JSONArray f=o.optJSONArray("folders"); if(f!=null)for(int i=0;i<f.length();i++)folders.add(f.getString(i)); } }catch(Exception ignored){} sort(); }
    public void saveIndex(){ try{ JSONObject o=new JSONObject(); JSONArray a=new JSONArray(); for(Doc d:docs)a.put(d.toJson()); o.put("docs",a); JSONArray f=new JSONArray(); for(String s:folders())f.put(s); o.put("folders",f); write(indexFile,o.toString()); }catch(Exception ignored){} }
    public void sort(){ Collections.sort(docs,(a,b)->Long.compare(b.updated,a.updated)); }
    public Doc create(String title,String type,String folder,int color){ Doc d=new Doc(); d.id=UUID.randomUUID().toString().substring(0,8); d.title=title; d.type=type; d.folder=folder==null?"All":folder; d.color=color; d.updated=System.currentTimeMillis();
        if(!type.equals("text")) write(docFile(d.id),new CanvasDoc().toJson()); else write(docFile(d.id),""); docs.add(0,d); saveIndex(); return d; }
    public Doc get(String id){ for(Doc d:docs) if(d.id.equals(id)) return d; return null; }
    public void touch(Doc d){ d.updated=System.currentTimeMillis(); sort(); saveIndex(); }
    public void delete(Doc d){ docs.remove(d); docFile(d.id).delete(); pdfFile(d.id).delete(); saveIndex(); }
    public Doc duplicate(Doc d){ Doc n=create(d.title+" copy",d.type,d.folder,d.color); try{ write(docFile(n.id),read(docFile(d.id))); if(pdfFile(d.id).exists()) copy(new FileInputStream(pdfFile(d.id)),pdfFile(n.id)); n.pdfPages=d.pdfPages; n.pdfName=d.pdfName; }catch(Exception ignored){} saveIndex(); return n; }
    public CanvasDoc loadCanvas(Doc d){ try{ return CanvasDoc.fromJson(read(docFile(d.id))); }catch(Exception e){ return new CanvasDoc(); } }
    public void saveCanvas(Doc d,CanvasDoc c){ write(docFile(d.id),c.toJson()); d.snippet=c.plainText(); touch(d); }
    public String importPdf(Context ctx,Uri uri,String name) throws Exception { Doc d=create(name==null?"Imported PDF":name.replaceAll("\\.pdf$",""),"pdf","All",0xFFFF9F0A);
        try(InputStream in=ctx.getContentResolver().openInputStream(uri)){ copy(in,pdfFile(d.id)); } d.pdfName=name; saveIndex(); return d.id; }
    public String importImageAsDoc(Context ctx,Uri uri) throws Exception { Doc d=create("Image whiteboard","whiteboard","All",0xFF30D158); CanvasDoc c=new CanvasDoc();
        String fn=d.id+"_img.jpg"; copyImage(ctx,uri,new File(mediaDir(),fn),1600); CanvasDoc.ImageObj im=new CanvasDoc.ImageObj(); im.file=fn; im.x=60;im.y=60; c.pages.get(0).images.add(im); saveCanvas(d,c); return d.id; }
    public static void copyImage(Context ctx,Uri uri,File out,int maxDim) throws Exception { try(InputStream in=ctx.getContentResolver().openInputStream(uri)){ android.graphics.Bitmap b=android.graphics.BitmapFactory.decodeStream(in);
        if(b==null) throw new Exception("Unsupported image"); float s=Math.min(1f,(float)maxDim/Math.max(b.getWidth(),b.getHeight())); if(s<1) b=android.graphics.Bitmap.createScaledBitmap(b,(int)(b.getWidth()*s),(int)(b.getHeight()*s),true);
        try(FileOutputStream o=new FileOutputStream(out)){ b.compress(android.graphics.Bitmap.CompressFormat.JPEG,88,o); } } }
    public static String read(File f) throws Exception { ByteArrayOutputStream b=new ByteArrayOutputStream(); try(FileInputStream in=new FileInputStream(f)){ copy(in,b); } return b.toString("UTF-8"); }
    public static void write(File f,String s){ try(FileOutputStream o=new FileOutputStream(f)){ o.write(s.getBytes("UTF-8")); }catch(Exception ignored){} }
    public static void copy(InputStream in,File out) throws Exception { try(FileOutputStream o=new FileOutputStream(out)){ copy(in,o); } }
    public static void copy(InputStream in,OutputStream o) throws Exception { byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0)o.write(buf,0,n); }
}
