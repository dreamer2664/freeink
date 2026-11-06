package app.freeink.ui;
import android.app.Activity; import android.graphics.*; import android.os.Bundle; import android.text.*; import android.text.style.*; import android.view.*; import android.widget.*; import app.freeink.data.DocStore; import app.freeink.util.Ui; import java.io.File;

public class TextNoteActivity extends Activity {
    DocStore store; DocStore.Doc doc; EditText edit;
    protected void onCreate(Bundle b){ super.onCreate(b); store=new DocStore(this); doc=store.get(getIntent().getStringExtra("id")); if(doc==null){finish();return;}
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Ui.bg(this)); root.setPadding(Ui.dp(this,16),Ui.dp(this,12),Ui.dp(this,16),0);
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
        Button back=Ui.btn(this,"‹ Library",Ui.card(this),Ui.ink(this)); back.setOnClickListener(v->finish()); bar.addView(back);
        TextView title=Ui.tv(this,doc.title,18,Ui.ink(this),true); LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1); tp.setMargins(Ui.dp(this,12),0,0,0); bar.addView(title,tp);
        Button share=Ui.btn(this,"Share",Ui.card(this),Ui.ink(this)); share.setOnClickListener(v->share()); bar.addView(share); root.addView(bar);
        LinearLayout fmt=new LinearLayout(this); fmt.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,8));
        fmt.addView(fmtBtn("H1",v->span(new RelativeSizeSpan(1.7f),new StyleSpan(Typeface.BOLD)))); fmt.addView(fmtBtn("B",v->span(new StyleSpan(Typeface.BOLD),null)));
        fmt.addView(fmtBtn("I",v->span(new StyleSpan(Typeface.ITALIC),null))); fmt.addView(fmtBtn("U",v->span(new UnderlineSpan(),null)));
        fmt.addView(fmtBtn("• List",v->insert("\n• "))); fmt.addView(fmtBtn("1. List",v->insert("\n1. "))); fmt.addView(fmtBtn("☐ Task",v->insert("\n☐ "))); fmt.addView(fmtBtn("Color",v->colorMenu()));
        root.addView(fmt);
        edit=new EditText(this); edit.setTextSize(17); edit.setTextColor(Ui.ink(this)); edit.setHint("Start writing…"); edit.setGravity(Gravity.TOP); edit.setBackground(Ui.round(this,Ui.card(this),16,Ui.line(this))); edit.setPadding(Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,16));
        try{ String html=DocStore.read(store.docFile(doc.id)); if(html!=null&&!html.isEmpty()) edit.setText(Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY)); }catch(Exception ignored){}
        root.addView(edit,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root); }
    Button fmtBtn(String t,View.OnClickListener o){ Button b=Ui.btn(this,t,Ui.card(this),Ui.ink(this)); b.setOnClickListener(o); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,Ui.dp(this,38)); lp.setMargins(0,0,Ui.dp(this,6),0); b.setLayoutParams(lp); return b; }
    void span(Object s1,Object s2){ int a=edit.getSelectionStart(),b=edit.getSelectionEnd(); if(a<0||b<=a){ Toast.makeText(this,"Select text first",Toast.LENGTH_SHORT).show(); return; } if(s1!=null)edit.getText().setSpan(s1,a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); if(s2!=null)edit.getText().setSpan(s2,a,b,Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); save(); }
    void insert(String s){ int a=Math.max(0,edit.getSelectionStart()); edit.getText().insert(a,s); save(); }
    void colorMenu(){ int[] cs={0xFF111111,0xFF0071E3,0xFFFF3B30,0xFF34C759,0xFFFF9F0A,0xFFBF5AF2}; String[] ns={"Black","Blue","Red","Green","Orange","Purple"};
        new android.app.AlertDialog.Builder(this).setTitle("Text color").setItems(ns,(d,w)->span(new ForegroundColorSpan(cs[w]),null)).show(); }
    void save(){ if(doc==null)return; DocStore.write(store.docFile(doc.id),Html.toHtml(edit.getText(),Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)); doc.snippet=edit.getText().toString().replaceAll("\\s+"," ").trim(); if(doc.snippet.length()>120)doc.snippet=doc.snippet.substring(0,120); store.touch(doc); }
    void share(){ save(); try{ File f=new File(new File(getCacheDir(),"shared"),doc.title.replaceAll("[^a-zA-Z0-9 ]","")+".txt"); new File(getCacheDir(),"shared").mkdirs(); DocStore.write(f,edit.getText().toString()); app.freeink.util.ExportUtil.share(this,f,"text/plain"); }catch(Exception e){ Toast.makeText(this,"Share failed",Toast.LENGTH_SHORT).show(); } }
    protected void onPause(){ super.onPause(); save(); }
}
