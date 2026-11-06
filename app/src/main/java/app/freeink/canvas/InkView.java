package app.freeink.canvas;
import android.content.*; import android.graphics.*; import android.view.*; import app.freeink.data.CanvasDoc; import java.io.File; import java.util.*;

public class InkView extends View {
    public enum Tool { PEN, HIGHLIGHTER, ERASER, LASSO, SELECT, TEXT, SHAPE, PAN }
    public interface Listener { void onChanged(); void onTextTap(CanvasDoc.TextObj t); void onNewText(float x,float y); }
    public Listener listener;
    public CanvasDoc doc; public int pageIndex=0; public boolean infinite=false; public String template="blank";
    public Tool tool=Tool.PEN; public int color=0xFF111111; public float penWidth=4f; public int shapeKind=0; public boolean shapeFill=false;
    public boolean fingerDraw=true;
    public Bitmap pdfBitmap; public File mediaDir;
    public float scale=1f, tx=40, ty=40; public static final float PW=1240, PH=1754;
    private RectF lassoBox; private boolean draggingGroup;
    private CanvasDoc.Stroke cur; private CanvasDoc.ShapeObj curShape; private Path lasso; private List<Object> lassoSel=new ArrayList<>();
    private Object selected; private float lastX,lastY, downWX,downWY; private boolean resizing, rotating;
    private final List<String> undoStack=new ArrayList<>(), redoStack=new ArrayList<>();
    private ScaleGestureDetector scaler; private boolean scaling; private final Map<String,Bitmap> bmpCache=new HashMap<>();
    private boolean stylusSeen=false;

    public InkView(Context c){ super(c); setBackgroundColor(0xFFE8E8ED);
        scaler=new ScaleGestureDetector(c,new ScaleGestureDetector.SimpleOnScaleGestureListener(){ public boolean onScale(ScaleGestureDetector d){
            float ns=Math.max(.15f,Math.min(5f,scale*d.getScaleFactor())); float f=ns/scale; scale=ns;
            tx=d.getFocusX()-(d.getFocusX()-tx)*f; ty=d.getFocusY()-(d.getFocusY()-ty)*f; invalidate(); return true; }
            public boolean onScaleBegin(ScaleGestureDetector d){ scaling=true; return true; } public void onScaleEnd(ScaleGestureDetector d){ scaling=false; } }); }
    public CanvasDoc.Page page(){ return doc.pages.get(Math.min(pageIndex,doc.pages.size()-1)); }
    public void setDoc(CanvasDoc d){ doc=d; template=d.template; undoStack.clear(); redoStack.clear(); selected=null; fitPage(); }
    public void fitPage(){ if(getWidth()==0)return; if(infinite){scale=1;tx=60;ty=60;} else { scale=Math.min((getWidth()-48)/PW,(getHeight()-48)/PH); tx=(getWidth()-PW*scale)/2; ty=(getHeight()-PH*scale)/2; } invalidate(); }
    protected void onSizeChanged(int w,int h,int a,int b){ super.onSizeChanged(w,h,a,b); if(doc!=null&&(tx==40)) fitPage(); }
    float wx(float sx){ return (sx-tx)/scale; } float wy(float sy){ return (sy-ty)/scale; }
    public void pushUndo(){ undoStack.add(doc.toJson()); if(undoStack.size()>60)undoStack.remove(0); redoStack.clear(); }
    public boolean undo(){ if(undoStack.isEmpty())return false; redoStack.add(doc.toJson()); String s=undoStack.remove(undoStack.size()-1); int pi=pageIndex; doc=CanvasDoc.fromJson(s); pageIndex=Math.min(pi,doc.pages.size()-1); changed(); return true; }
    public boolean redo(){ if(redoStack.isEmpty())return false; undoStack.add(doc.toJson()); String s=redoStack.remove(redoStack.size()-1); int pi=pageIndex; doc=CanvasDoc.fromJson(s); pageIndex=Math.min(pi,doc.pages.size()-1); changed(); return true; }
    private void changed(){ invalidate(); if(listener!=null)listener.onChanged(); }

    private Bitmap bmp(String f){ if(!bmpCache.containsKey(f)){ Bitmap b=BitmapFactory.decodeFile(new File(mediaDir,f).getAbsolutePath()); bmpCache.put(f,b);} return bmpCache.get(f); }
    public void clearBmpCache(){ bmpCache.clear(); }

    protected void onDraw(Canvas cv){
        super.onDraw(cv); if(doc==null)return; cv.save(); cv.translate(tx,ty); cv.scale(scale,scale);
        CanvasDoc.Page pg=page();
        if(!infinite){ // page sheet
            Paint sh=new Paint(); sh.setColor(0x33000000); cv.drawRoundRect(new RectF(14,18,PW+14,PH+18),18,18,sh);
            Paint bg=new Paint(); bg.setColor(template.equals("dark")?0xFF1C1C1E:Color.WHITE); cv.drawRoundRect(new RectF(0,0,PW,PH),18,18,bg);
            drawTemplate(cv,0,0,PW,PH);
            if(pdfBitmap!=null) cv.drawBitmap(pdfBitmap,null,new RectF(0,0,PW,PH),null);
            cv.save(); cv.clipRect(0,0,PW,PH); drawPage(cv,pg); cv.restore();
        } else {
            drawInfiniteBg(cv);
            drawPage(cv,pg);
        }
        // selection box
        if(selected!=null){ RectF r=bounds(selected); if(r!=null){ Paint p=new Paint(); p.setStyle(Paint.Style.STROKE); p.setColor(0xFF0071E3); p.setStrokeWidth(2/scale); cv.drawRoundRect(r,8,8,p);
            p.setStyle(Paint.Style.FILL); cv.drawCircle(r.right,r.bottom,14/scale,p); p.setColor(Color.WHITE); cv.drawCircle(r.right,r.top,14/scale,p);} }
        if(lasso!=null){ Paint p=new Paint(); p.setStyle(Paint.Style.STROKE); p.setColor(0xFF0071E3); p.setStrokeWidth(2/scale); cv.drawPath(lasso,p); }
        cv.restore();
    }
    private void drawTemplate(Canvas cv,float x0,float y0,float w,float h){ Paint p=new Paint(); p.setColor(template.equals("dark")?0xFF3A3A3C:0xFFDDE6F0); p.setStrokeWidth(1.5f);
        if(template.equals("ruled")) for(float y=y0+90;y<y0+h;y+=64) cv.drawLine(x0+50,y,x0+w-50,y,p);
        else if(template.equals("grid")){ for(float x=x0+48;x<x0+w;x+=48)cv.drawLine(x,y0,x,y0+h,p); for(float y=y0+48;y<y0+h;y+=48)cv.drawLine(x0,y,x0+w,y,p); }
        else if(template.equals("dotted")){ p.setStyle(Paint.Style.FILL); for(float x=x0+48;x<x0+w;x+=48)for(float y=y0+48;y<y0+h;y+=48)cv.drawCircle(x,y,2.2f,p); } }
    private void drawInfiniteBg(Canvas cv){ Paint bg=new Paint(); bg.setColor(template.equals("dark")?0xFF1C1C1E:0xFFF5F5F7); cv.drawRect(wx(0)-10,wy(0)-10,wx(getWidth())+10,wy(getHeight())+10,bg);
        float step=48; float sx=(float)Math.floor(wx(0)/step)*step, sy=(float)Math.floor(wy(0)/step)*step; Paint p=new Paint();
        if(template.equals("grid")){ p.setColor(0xFFD9D9DE); p.setStrokeWidth(1/scale); for(float x=sx;x<wx(getWidth());x+=step)cv.drawLine(x,wy(0),x,wy(getHeight()),p); for(float y=sy;y<wy(getHeight());y+=step)cv.drawLine(wx(0),y,wx(getWidth()),y,p); }
        else if(!template.equals("blank")&&!template.equals("dark")){ p.setColor(0xFFC7C7CC); p.setStyle(Paint.Style.FILL); for(float x=sx;x<wx(getWidth());x+=step)for(float y=sy;y<wy(getHeight());y+=step)cv.drawCircle(x,y,2.4f,p); } }
    private void drawPage(Canvas cv,CanvasDoc.Page pg){
        for(CanvasDoc.ImageObj im:pg.images){ Bitmap b=bmp(im.file); if(b==null)continue; cv.save(); cv.translate(im.x+im.w/2,im.y+im.h/2); cv.rotate(im.rot); cv.drawBitmap(b,null,new RectF(-im.w/2,-im.h/2,im.w/2,im.h/2),null); cv.restore(); }
        Paint p=new Paint(); p.setAntiAlias(true); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        for(CanvasDoc.ShapeObj s:pg.shapes) drawShape(cv,s,p);
        for(CanvasDoc.Stroke s:pg.strokes) drawStroke(cv,s,p);
        if(cur!=null) drawStroke(cv,cur,p);
        Paint tp=new Paint(); tp.setAntiAlias(true);
        for(CanvasDoc.TextObj t:pg.texts){ tp.setColor(t.color); tp.setTextSize(t.size); tp.setTypeface(t.bold?Typeface.DEFAULT_BOLD:Typeface.DEFAULT);
            String[] lines=t.text.split("\n"); float y=t.y; for(String ln:lines){ cv.drawText(ln,t.x,y,tp); y+=t.size*1.25f; } }
    }
    private void drawStroke(Canvas cv,CanvasDoc.Stroke s,Paint p){ p.setStyle(Paint.Style.STROKE); if(s.highlighter){ p.setColor((s.color&0x00FFFFFF)|0x55000000); }
        else p.setColor(s.color);
        if(s.pts.size()==1){ p.setStyle(Paint.Style.FILL); cv.drawCircle(s.pts.get(0).x,s.pts.get(0).y,s.width/2,p); return; }
        for(int i=1;i<s.pts.size();i++){ CanvasDoc.Pt a=s.pts.get(i-1),b=s.pts.get(i); p.setStrokeWidth(Math.max(1f,s.width*((a.p+b.p)/2f))); cv.drawLine(a.x,a.y,b.x,b.y,p); } }
    private void drawShape(Canvas cv,CanvasDoc.ShapeObj s,Paint p){ p.setColor(s.color); p.setStrokeWidth(s.width); p.setStyle(s.fill?Paint.Style.FILL_AND_STROKE:Paint.Style.STROKE);
        RectF r=new RectF(Math.min(s.x,s.x2),Math.min(s.y,s.y2),Math.max(s.x,s.x2),Math.max(s.y,s.y2));
        switch(s.kind){ case 1: cv.drawOval(r,p); break; case 2: cv.drawLine(s.x,s.y,s.x2,s.y2,p); break;
            case 3: cv.drawLine(s.x,s.y,s.x2,s.y2,p); float ang=(float)Math.atan2(s.y2-s.y,s.x2-s.x); float L=34;
                cv.drawLine(s.x2,s.y2,s.x2-L*(float)Math.cos(ang-.45),s.y2-L*(float)Math.sin(ang-.45),p); cv.drawLine(s.x2,s.y2,s.x2-L*(float)Math.cos(ang+.45),s.y2-L*(float)Math.sin(ang+.45),p); break;
            case 4: Path path=new Path(); path.moveTo((r.left+r.right)/2,r.top); path.lineTo(r.right,r.bottom); path.lineTo(r.left,r.bottom); path.close(); cv.drawPath(path,p); break;
            default: cv.drawRoundRect(r,10,10,p); } if(curShape==s) {} }

    private RectF bounds(Object o){ if(o instanceof CanvasDoc.TextObj){ CanvasDoc.TextObj t=(CanvasDoc.TextObj)o; Paint p=new Paint(); p.setTextSize(t.size); float w=0; for(String l:t.text.split("\n"))w=Math.max(w,p.measureText(l)); return new RectF(t.x-8,t.y-t.size-8,t.x+w+8,t.y+t.size); }
        if(o instanceof CanvasDoc.ShapeObj){ CanvasDoc.ShapeObj s=(CanvasDoc.ShapeObj)o; return new RectF(Math.min(s.x,s.x2),Math.min(s.y,s.y2),Math.max(s.x,s.x2),Math.max(s.y,s.y2)); }
        if(o instanceof CanvasDoc.ImageObj){ CanvasDoc.ImageObj i=(CanvasDoc.ImageObj)o; return new RectF(i.x,i.y,i.x+i.w,i.y+i.h); }
        if(o instanceof CanvasDoc.Stroke){ CanvasDoc.Stroke s=(CanvasDoc.Stroke)o; RectF r=new RectF(1e9f,1e9f,-1e9f,-1e9f); for(CanvasDoc.Pt pt:s.pts){r.left=Math.min(r.left,pt.x);r.top=Math.min(r.top,pt.y);r.right=Math.max(r.right,pt.x);r.bottom=Math.max(r.bottom,pt.y);} return r; } return null; }
    private Object hit(float x,float y){ CanvasDoc.Page pg=page();
        for(int i=pg.images.size()-1;i>=0;i--){ RectF r=bounds(pg.images.get(i)); if(r.contains(x,y))return pg.images.get(i); }
        for(int i=pg.texts.size()-1;i>=0;i--){ if(bounds(pg.texts.get(i)).contains(x,y))return pg.texts.get(i); }
        for(int i=pg.shapes.size()-1;i>=0;i--){ if(bounds(pg.shapes.get(i)).contains(x,y))return pg.shapes.get(i); }
        for(int i=pg.strokes.size()-1;i>=0;i--){ for(CanvasDoc.Pt pt:pg.strokes.get(i).pts) if(Math.hypot(pt.x-x,pt.y-y)<24) return pg.strokes.get(i); }
        return null; }
    private void moveObj(Object o,float dx,float dy){ if(o instanceof CanvasDoc.TextObj){((CanvasDoc.TextObj)o).x+=dx;((CanvasDoc.TextObj)o).y+=dy;}
        else if(o instanceof CanvasDoc.ShapeObj){((CanvasDoc.ShapeObj)o).x+=dx;((CanvasDoc.ShapeObj)o).x2+=dx;((CanvasDoc.ShapeObj)o).y+=dy;((CanvasDoc.ShapeObj)o).y2+=dy;}
        else if(o instanceof CanvasDoc.ImageObj){((CanvasDoc.ImageObj)o).x+=dx;((CanvasDoc.ImageObj)o).y+=dy;}
        else if(o instanceof CanvasDoc.Stroke){ for(CanvasDoc.Pt p:((CanvasDoc.Stroke)o).pts){p.x+=dx;p.y+=dy;} } }
    public void deleteSelected(){ if(selected==null)return; pushUndo(); CanvasDoc.Page pg=page(); pg.texts.remove(selected); pg.shapes.remove(selected); pg.images.remove(selected); pg.strokes.remove(selected); selected=null; changed(); }
    public void rotateSelected(){ if(selected instanceof CanvasDoc.ImageObj){ pushUndo(); ((CanvasDoc.ImageObj)selected).rot=(((CanvasDoc.ImageObj)selected).rot+15)%360; changed(); } }
    public Object selectedObj(){ return selected; }

    public boolean onTouchEvent(MotionEvent e){
        scaler.onTouchEvent(e); if(scaling||e.getPointerCount()>1){ cur=null; return true; }
        int toolType=e.getToolType(0); if(toolType==MotionEvent.TOOL_TYPE_STYLUS||toolType==MotionEvent.TOOL_TYPE_ERASER) stylusSeen=true;
        if(stylusSeen&&!fingerDraw&&toolType==MotionEvent.TOOL_TYPE_FINGER&&tool!=Tool.PAN) return true; // palm rejection
        float x=wx(e.getX()), y=wy(e.getY());
        switch(e.getActionMasked()){
        case MotionEvent.ACTION_DOWN: pushPending=true; lastX=x; lastY=y; downWX=x; downWY=y; preStrokeJson=doc.toJson();
            if(tool==Tool.LASSO&&!lassoSel.isEmpty()&&lassoBox!=null&&lassoBox.contains(x,y)){ draggingGroup=true; break; }
            if(tool==Tool.LASSO){ lassoSel.clear(); lassoBox=null; }
            if(tool==Tool.SELECT||tool==Tool.LASSO&&false){} 
            if(tool==Tool.SELECT){ selected=hit(x,y); RectF r=selected==null?null:bounds(selected); resizing=r!=null&&Math.hypot(x-r.right,y-r.bottom)<36/scale+18; rotating=r!=null&&Math.hypot(x-r.right,y-r.top)<36/scale+18; if(selected!=null)pushUndo(); invalidate(); }
            else if(tool==Tool.PEN||tool==Tool.HIGHLIGHTER||toolType==MotionEvent.TOOL_TYPE_ERASER&&tool==Tool.ERASER){ }
            if(tool==Tool.PEN||tool==Tool.HIGHLIGHTER){ cur=new CanvasDoc.Stroke(); cur.color=color; cur.width=tool==Tool.HIGHLIGHTER?penWidth*5:penWidth; cur.highlighter=tool==Tool.HIGHLIGHTER; cur.pts.add(new CanvasDoc.Pt(x,y,pressure(e))); }
            else if(tool==Tool.ERASER){ eraseAt(x,y); }
            else if(tool==Tool.SHAPE){ curShape=new CanvasDoc.ShapeObj(); curShape.kind=shapeKind; curShape.color=color; curShape.width=penWidth; curShape.fill=shapeFill; curShape.x=x;curShape.y=y;curShape.x2=x;curShape.y2=y; page().shapes.add(curShape); }
            else if(tool==Tool.LASSO){ lasso=new Path(); lasso.moveTo(x,y); lassoSel.clear(); }
            break;
        case MotionEvent.ACTION_MOVE: float dx=x-lastX, dy=y-lastY;
            if(tool==Tool.PAN){ tx+=e.getX()-lastSX; ty+=e.getY()-lastSY; }
            else if(cur!=null){ cur.pts.add(new CanvasDoc.Pt(x,y,pressure(e))); }
            else if(tool==Tool.ERASER){ eraseAt(x,y); }
            else if(curShape!=null){ curShape.x2=x; curShape.y2=y; }
            else if(tool==Tool.LASSO&&lasso!=null){ lasso.lineTo(x,y); }
            else if(selected!=null&&(tool==Tool.SELECT)){ if(rotating){ /* rotation via toolbar */ } else if(resizing){ resize(selected,dx,dy); } else moveObj(selected,dx,dy); changed(); }
            else if(draggingGroup){ for(Object o:lassoSel)moveObj(o,dx,dy); if(lassoBox!=null)lassoBox.offset(dx,dy); changed(); }
            lastX=x; lastY=y; invalidate(); break;
        case MotionEvent.ACTION_UP: case MotionEvent.ACTION_CANCEL:
            if(cur!=null){ pushUndoBefore(); page().strokes.add(cur); cur=null; changed(); }
            else if(curShape!=null){ pushUndoBefore(); curShape=null; changed(); }
            else if(tool==Tool.TEXT){ if(listener!=null)listener.onNewText(x,y); }
            else if(tool==Tool.LASSO&&lasso!=null){ selectInLasso(); lasso=null; pushUndoBefore(); invalidate(); }
            else if(tool==Tool.SELECT&&selected instanceof CanvasDoc.TextObj&&Math.hypot(x-downWX,y-downWY)<6){ if(listener!=null)listener.onTextTap((CanvasDoc.TextObj)selected); }
            if(draggingGroup||tool==Tool.ERASER) pushUndoBefore();
            draggingGroup=false; pushPending=false; break; }
        lastSX=e.getX(); lastSY=e.getY(); return true; }
    private float lastSX,lastSY; private boolean pushPending; private String preStrokeJson;
    private void pushUndoBefore(){ /* snapshot taken at stroke start via pre-edit copy */ if(preStrokeJson!=null){ undoStack.add(preStrokeJson); if(undoStack.size()>60)undoStack.remove(0); redoStack.clear(); preStrokeJson=null; } }
    private float pressure(MotionEvent e){ float p=e.getPressure(); if(p<=0||p>1.5f||e.getToolType(0)==MotionEvent.TOOL_TYPE_FINGER) return .8f; return Math.max(.25f,Math.min(1.4f,p)); }
    private void eraseAt(float x,float y){ CanvasDoc.Page pg=page(); List<CanvasDoc.Stroke> rm=new ArrayList<>();
        for(CanvasDoc.Stroke s:pg.strokes) for(CanvasDoc.Pt p:s.pts) if(Math.hypot(p.x-x,p.y-y)<26){ rm.add(s); break; }
        if(!rm.isEmpty()){ if(preStrokeJson==null)preStrokeJson=doc.toJson(); pg.strokes.removeAll(rm); changed(); } }
    private void resize(Object o,float dx,float dy){ if(o instanceof CanvasDoc.ImageObj){ CanvasDoc.ImageObj i=(CanvasDoc.ImageObj)o; i.w=Math.max(40,i.w+dx); i.h=Math.max(40,i.h+dy); }
        else if(o instanceof CanvasDoc.ShapeObj){ ((CanvasDoc.ShapeObj)o).x2+=dx; ((CanvasDoc.ShapeObj)o).y2+=dy; }
        else if(o instanceof CanvasDoc.TextObj){ ((CanvasDoc.TextObj)o).size=Math.max(14,((CanvasDoc.TextObj)o).size+dy*.4f); } }
    private void selectInLasso(){ // bounding-box approximation of lasso
        RectF box=new RectF(); lasso.computeBounds(box,true); lassoBox=box; CanvasDoc.Page pg=page(); lassoSel.clear();
        for(CanvasDoc.Stroke s:pg.strokes){ RectF r=bounds(s); if(r!=null&&box.contains(r))lassoSel.add(s); }
        for(CanvasDoc.TextObj t:pg.texts) if(box.contains(bounds(t)))lassoSel.add(t);
        for(CanvasDoc.ShapeObj s:pg.shapes) if(box.contains(bounds(s)))lassoSel.add(s);
        for(CanvasDoc.ImageObj i:pg.images) if(box.contains(bounds(i)))lassoSel.add(i);
        if(!lassoSel.isEmpty()&&listener!=null)listener.onChanged(); }
    // called by parent before edits that need snapshot: capture lazily on down
    { }
    public void beginEditSnapshot(){ preStrokeJson=doc.toJson(); }
}
