package app.freeink.ui;
import android.app.*; import android.content.*; import android.graphics.Bitmap; import android.net.Uri; import android.os.*; import android.view.*; import android.widget.*; import app.freeink.canvas.InkView; import app.freeink.data.*; import app.freeink.pdf.PdfHelper; import app.freeink.util.*; import java.io.File;

public class EditorActivity extends Activity implements InkView.Listener {
    DocStore store; DocStore.Doc doc; CanvasDoc canvas; InkView ink; PdfHelper pdf; TextView pageLabel,status; Handler handler=new Handler(Looper.getMainLooper()); Runnable saver;
    static final int REQ_IMG=7; Buttons toolBtns=new Buttons();
    static class Buttons { java.util.Map<String,Button> m=new java.util.HashMap<>(); }
    protected void onCreate(Bundle b){ super.onCreate(b); store=new DocStore(this); doc=store.get(getIntent().getStringExtra("id")); if(doc==null){finish();return;}
        canvas=store.loadCanvas(doc); if(doc.type.equals("note")&&canvas.pages.size()==1&&canvas.plainText().isEmpty()&&canvas.pages.get(0).strokes.isEmpty()) canvas.template=getSharedPreferences("freeink",0).getString("template","blank");
        build(); if(doc.type.equals("pdf")) openPdf(); }
    void build(){ LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Ui.bg(this));
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(Ui.dp(this,10),Ui.dp(this,8),Ui.dp(this,10),Ui.dp(this,8)); top.setBackgroundColor(Ui.card(this));
        Button back=Ui.btn(this,"‹ Library",Ui.card(this),Ui.ink(this)); back.setOnClickListener(v->{save();finish();}); top.addView(back);
        TextView title=Ui.tv(this,doc.title,17,Ui.ink(this),true); title.setOnClickListener(v->rename()); LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1); tp.setMargins(Ui.dp(this,10),0,0,0); top.addView(title,tp);
        pageLabel=Ui.tv(this,"",13,Ui.sub(this),false); top.addView(pageLabel);
        Button prev=Ui.btn(this,"‹",Ui.card(this),Ui.ink(this)); prev.setOnClickListener(v->gotoPage(ink.pageIndex-1)); top.addView(prev);
        Button next=Ui.btn(this,"›",Ui.card(this),Ui.ink(this)); next.setOnClickListener(v->gotoPage(ink.pageIndex+1)); top.addView(next);
        Button pages=Ui.btn(this,"Pages",Ui.card(this),Ui.ink(this)); pages.setOnClickListener(v->pageMenu()); top.addView(pages);
        Button undo=Ui.btn(this,"↩",Ui.card(this),Ui.ink(this)); undo.setOnClickListener(v->{ink.undo(); saveSoon();}); top.addView(undo);
        Button redo=Ui.btn(this,"↪",Ui.card(this),Ui.ink(this)); redo.setOnClickListener(v->{ink.redo(); saveSoon();}); top.addView(redo);
        Button more=Ui.btn(this,"•••",Ui.card(this),Ui.ink(this)); more.setOnClickListener(v->moreMenu()); top.addView(more); root.addView(top);
        ink=new InkView(this); ink.listener=this; ink.mediaDir=store.mediaDir(); ink.infinite=doc.type.equals("whiteboard"); ink.fingerDraw=getSharedPreferences("freeink",0).getBoolean("finger",true); ink.penWidth=getSharedPreferences("freeink",0).getFloat("penWidth",4);
        ink.setDoc(canvas); root.addView(ink,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(toolBar()); status=Ui.tv(this,"Saved · "+Ui.fmtDate(doc.updated),11,Ui.sub(this),false); status.setPadding(Ui.dp(this,12),Ui.dp(this,3),0,Ui.dp(this,5)); root.addView(status);
        setContentView(root); updatePageLabel(); }
    View toolBar(){ LinearLayout wrap=new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL); wrap.setBackgroundColor(Ui.card(this)); wrap.setPadding(Ui.dp(this,8),Ui.dp(this,6),Ui.dp(this,8),Ui.dp(this,8));
        LinearLayout tools=new LinearLayout(this);
        addTool(tools,"Pen",InkView.Tool.PEN); addTool(tools,"Marker",InkView.Tool.HIGHLIGHTER); addTool(tools,"Eraser",InkView.Tool.ERASER); addTool(tools,"Lasso",InkView.Tool.LASSO); addTool(tools,"Select",InkView.Tool.SELECT); addTool(tools,"Text",InkView.Tool.TEXT); addTool(tools,"Shape",InkView.Tool.SHAPE); addTool(tools,"Hand",InkView.Tool.PAN);
        Button img=Ui.btn(this,"Image",Ui.card(this),Ui.ink(this)); img.setOnClickListener(v->pickImage()); tools.addView(img);
        Button del=Ui.btn(this,"Delete obj.",0xFFFF3B30,0xFFFFFFFF); del.setOnClickListener(v->{ink.deleteSelected(); saveSoon();}); tools.addView(del);
        Button rot=Ui.btn(this,"Rotate",Ui.card(this),Ui.ink(this)); rot.setOnClickListener(v->{ink.rotateSelected(); saveSoon();}); tools.addView(rot);
        HorizontalScrollView hs=new HorizontalScrollView(this); hs.addView(tools); wrap.addView(hs);
        LinearLayout colors=new LinearLayout(this); colors.setPadding(0,Ui.dp(this,8),0,0);
        int[] cs={0xFF111111,0xFF0071E3,0xFFFF3B30,0xFFFF9F0A,0xFF34C759,0xFFBF5AF2,0xFFFF375F,0xFF8E8E93,0xFFFFFFFF};
        for(int c:cs){ Button b=new Button(this); b.setBackground(Ui.round(this,c,20,c==0xFFFFFFFF?Ui.line(this):c)); b.setOnClickListener(v->{ink.color=c;}); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(Ui.dp(this,34),Ui.dp(this,34)); lp.setMargins(0,0,Ui.dp(this,8),0); colors.addView(b,lp); }
        SeekBar thick=new SeekBar(this); thick.setMax(15); thick.setProgress(3); thick.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ public void onProgressChanged(SeekBar s,int v,boolean u){ ink.penWidth=v+1; } public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){} });
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(0,Ui.dp(this,36),1); tlp.setMargins(Ui.dp(this,8),0,0,0); colors.addView(thick,tlp); wrap.addView(colors); selectTool(InkView.Tool.PEN); return wrap; }
    void addTool(LinearLayout parent,String label,InkView.Tool t){ Button b=Ui.btn(this,label,Ui.card(this),Ui.ink(this)); b.setOnClickListener(v->{selectTool(t); if(t==InkView.Tool.SHAPE) shapeMenu();}); toolBtns.m.put(label,b); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,Ui.dp(this,38)); lp.setMargins(0,0,Ui.dp(this,6),0); parent.addView(b,lp); }
    void selectTool(InkView.Tool t){ ink.tool=t; for(java.util.Map.Entry<String,Button> e:toolBtns.m.entrySet()){ boolean on=e.getValue().getText().toString().equals(labelOf(t)); e.getValue().setBackground(Ui.round(this,on?Ui.ACCENT:Ui.card(this),10,0)); e.getValue().setTextColor(on?0xFFFFFFFF:Ui.ink(this)); } }
    String labelOf(InkView.Tool t){ switch(t){case PEN:return "Pen";case HIGHLIGHTER:return "Marker";case ERASER:return "Eraser";case LASSO:return "Lasso";case SELECT:return "Select";case TEXT:return "Text";case SHAPE:return "Shape";default:return "Hand";} }
    void shapeMenu(){ String[] s={"Rectangle","Ellipse","Line","Arrow","Triangle","Filled toggle"}; new AlertDialog.Builder(this).setTitle("Shape").setItems(s,(d,w)->{ if(w==5){ ink.shapeFill=!ink.shapeFill; toast("Fill "+(ink.shapeFill?"on":"off")); } else ink.shapeKind=w; }).show(); }
    void pageMenu(){ String[] s={"Add page","Duplicate page","Delete page","Template…","Fit page"}; new AlertDialog.Builder(this).setTitle("Pages").setItems(s,(d,w)->{ switch(w){case 0: ink.pushUndo(); canvas.pages.add(ink.pageIndex+1,new CanvasDoc.Page()); ink.setDoc(canvas); gotoPage(ink.pageIndex+1); break;case 1: ink.pushUndo(); canvas.pages.add(ink.pageIndex+1,CanvasDoc.Page.fromJsonSafe(canvas.pages.get(ink.pageIndex))); ink.setDoc(canvas); gotoPage(ink.pageIndex+1); break;case 2: if(canvas.pages.size()>1){ink.pushUndo(); canvas.pages.remove(ink.pageIndex); ink.setDoc(canvas); gotoPage(Math.max(0,ink.pageIndex-1));} break;case 3: templateMenu(); break;case 4: ink.fitPage(); break;} saveSoon(); }).show(); }
    void templateMenu(){ String[] s={"blank","ruled","dotted","grid","dark"}; new AlertDialog.Builder(this).setTitle("Paper template").setItems(s,(d,w)->{ ink.pushUndo(); canvas.template=s[w]; ink.template=s[w]; ink.invalidate(); saveSoon(); }).show(); }
    void moreMenu(){ String[] s={"Export PNG","Export PDF","Share PDF","Rename","Add image…"}; new AlertDialog.Builder(this).setTitle(doc.title).setItems(s,(d,w)->{ try{ switch(w){case 0: File f=ExportUtil.exportPng(this,store,doc,canvas); ExportUtil.share(this,f,"image/png"); break;case 1: case 2: File f2=ExportUtil.exportPdf(this,store,doc,canvas); ExportUtil.share(this,f2,"application/pdf"); toast("PDF exported — choose Save/Share in the share sheet"); break;case 3: rename(); break;case 4: pickImage(); break; } }catch(Exception e){ toast("Export failed: "+e.getMessage()); } }).show(); }
    void rename(){ EditText e=new EditText(this); e.setText(doc.title); new AlertDialog.Builder(this).setTitle("Rename").setView(e).setPositiveButton("Save",(a,b2)->{ doc.title=e.getText().toString(); save(); recreate(); }).setNegativeButton("Cancel",null).show(); }
    void openPdf(){ new Thread(()->{ try{ pdf=new PdfHelper(store.pdfFile(doc.id)); doc.pdfPages=pdf.pages(); while(canvas.pages.size()<doc.pdfPages) canvas.pages.add(new CanvasDoc.Page()); runOnUiThread(()->{ loadPdfPage(0); updatePageLabel(); }); }catch(Exception e){ runOnUiThread(()->toast("Could not open PDF: "+e.getMessage())); } }).start(); }
    void loadPdfPage(int i){ if(pdf==null)return; new Thread(()->{ Bitmap b=pdf.page(i,1240); runOnUiThread(()->{ ink.pdfBitmap=b; ink.invalidate(); }); }).start(); }
    void gotoPage(int i){ if(i<0||i>=canvas.pages.size())return; save(); ink.pageIndex=i; if(doc.type.equals("pdf")) loadPdfPage(i); ink.invalidate(); updatePageLabel(); }
    void updatePageLabel(){ if(pageLabel!=null) pageLabel.setText("Page "+(ink.pageIndex+1)+" / "+canvas.pages.size()+"   "); }
    void pickImage(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,REQ_IMG); }
    protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(req!=REQ_IMG||res!=RESULT_OK||data==null||data.getData()==null)return; Uri u=data.getData();
        new Thread(()->{ try{ String fn=doc.id+"_"+System.currentTimeMillis()+".jpg"; DocStore.copyImage(this,u,new File(store.mediaDir(),fn),1600); runOnUiThread(()->{ ink.pushUndo(); CanvasDoc.ImageObj im=new CanvasDoc.ImageObj(); im.file=fn; im.x=ink.infinite?100:120; im.y=140; ink.page().images.add(im); ink.clearBmpCache(); ink.invalidate(); saveSoon(); }); }catch(Exception e){ runOnUiThread(()->toast("Image import failed")); } }).start(); }
    public void onChanged(){ saveSoon(); } public void onNewText(float x,float y){ editText(null,x,y); }
    public void onTextTap(CanvasDoc.TextObj t){ editText(t,0,0); }
    void editText(CanvasDoc.TextObj existing,float x,float y){ EditText e=new EditText(this); if(existing!=null)e.setText(existing.text); e.setHint("Text");
        CheckBox bold=new CheckBox(this); bold.setText("Bold"); if(existing!=null)bold.setChecked(existing.bold);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.addView(e); box.addView(bold);
        new AlertDialog.Builder(this).setTitle(existing==null?"Add text":"Edit text").setView(box).setPositiveButton("Done",(d,w)->{ ink.pushUndo(); if(existing==null){ CanvasDoc.TextObj t=new CanvasDoc.TextObj(); t.x=x; t.y=y; t.color=ink.color; t.text=e.getText().toString(); t.bold=bold.isChecked(); ink.page().texts.add(t); } else { existing.text=e.getText().toString(); existing.bold=bold.isChecked(); } ink.invalidate(); saveSoon(); }).setNegativeButton("Cancel",null).show(); }
    void saveSoon(){ status.setText("Editing…"); if(saver!=null)handler.removeCallbacks(saver); saver=this::save; handler.postDelayed(saver,600); }
    void save(){ store.saveCanvas(doc,canvas); if(status!=null)status.setText("Saved · "+Ui.fmtDate(doc.updated)); updatePageLabel(); }
    protected void onPause(){ super.onPause(); save(); } protected void onDestroy(){ super.onDestroy(); if(pdf!=null)pdf.close(); }
    void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_SHORT).show(); }
}
