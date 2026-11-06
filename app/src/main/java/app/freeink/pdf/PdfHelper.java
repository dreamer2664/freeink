package app.freeink.pdf;
import android.graphics.*; import android.graphics.pdf.PdfRenderer; import android.os.ParcelFileDescriptor; import java.io.File; import java.util.*;

public class PdfHelper {
    private PdfRenderer renderer; private PdfRenderer.Page current; private final Map<Integer,Bitmap> cache=new HashMap<>();
    public PdfHelper(File f) throws Exception { renderer=new PdfRenderer(ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY)); }
    public int pages(){ return renderer==null?0:renderer.getPageCount(); }
    public synchronized Bitmap page(int i,int targetW){
        if(cache.containsKey(i)) return cache.get(i);
        try{ if(current!=null)current.close(); current=renderer.openPage(i);
            float s=(float)targetW/current.getWidth(); int w=targetW,h=Math.max(1,(int)(current.getHeight()*s));
            Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); b.eraseColor(Color.WHITE);
            current.render(b,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY); current.close(); current=null;
            if(cache.size()>3) cache.clear(); cache.put(i,b); return b;
        }catch(Exception e){ return null; } }
    public int[] pageSize(int i){ try{ PdfRenderer.Page p=renderer.openPage(i); int[] r={p.getWidth(),p.getHeight()}; p.close(); return r; }catch(Exception e){ return new int[]{1240,1754}; } }
    public void close(){ try{ if(current!=null)current.close(); if(renderer!=null)renderer.close(); }catch(Exception ignored){} }
}
