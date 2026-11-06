package app.freeink.util;
import android.content.*; import android.graphics.*; import android.graphics.pdf.PdfDocument; import app.freeink.canvas.InkView; import app.freeink.data.*; import app.freeink.pdf.PdfHelper; import java.io.*;

public class ExportUtil {
    /** Render one page to a bitmap by reusing an off-screen InkView. */
    public static Bitmap renderPage(Context c,DocStore store,DocStore.Doc doc,CanvasDoc cd,int page){
        InkView v=new InkView(c); v.mediaDir=store.mediaDir(); v.infinite=doc.type.equals("whiteboard"); v.doc=cd; v.pageIndex=page; v.template=cd.template;
        if(doc.type.equals("pdf")){ try{ PdfHelper h=new PdfHelper(store.pdfFile(doc.id)); v.pdfBitmap=h.page(page,1240); v.layout(0,0,1240,1754); v.scale=1; v.tx=0; v.ty=0; Bitmap b=Bitmap.createBitmap(1240,1754,Bitmap.Config.ARGB_8888); v.draw(new Canvas(b)); h.close(); return b; }catch(Exception ignored){} }
        int w=v.infinite?1600:1240, h=v.infinite?1200:1754; v.layout(0,0,w,h); v.scale=v.infinite?1:1; v.tx=0; v.ty=0;
        Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); b.eraseColor(Color.WHITE); v.draw(new Canvas(b)); return b; }
    public static File exportPng(Context c,DocStore store,DocStore.Doc doc,CanvasDoc cd) throws Exception {
        File out=new File(new File(c.getCacheDir(),"shared"),san(doc.title)+".png"); new File(c.getCacheDir(),"shared").mkdirs();
        Bitmap b=renderPage(c,store,doc,cd,0); try(FileOutputStream o=new FileOutputStream(out)){ b.compress(Bitmap.CompressFormat.PNG,100,o); } return out; }
    public static File exportPdf(Context c,DocStore store,DocStore.Doc doc,CanvasDoc cd) throws Exception {
        File out=new File(new File(c.getCacheDir(),"shared"),san(doc.title)+".pdf"); new File(c.getCacheDir(),"shared").mkdirs();
        PdfDocument pdf=new PdfDocument();
        for(int i=0;i<cd.pages.size();i++){ Bitmap b=renderPage(c,store,doc,cd,i);
            PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(b.getWidth(),b.getHeight(),i+1).create(); PdfDocument.Page p=pdf.startPage(info);
            p.getCanvas().drawBitmap(b,0,0,null); pdf.finishPage(p); }
        try(FileOutputStream o=new FileOutputStream(out)){ pdf.writeTo(o); } pdf.close(); return out; }
    private static String san(String s){ return s.replaceAll("[^a-zA-Z0-9-_ ]","").trim().replace(' ','_'); }
    public static void share(Context c,File f,String mime){ Intent i=new Intent(Intent.ACTION_SEND); i.setType(mime); i.putExtra(Intent.EXTRA_STREAM,ShareProvider.uriFor(c,f)); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); c.startActivity(Intent.createChooser(i,"Share via")); }
}
