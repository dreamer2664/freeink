package app.freeink.ui;
import android.app.*; import android.content.*; import android.net.Uri; import android.os.Bundle; import android.provider.OpenableColumns; import android.database.Cursor; import android.view.*; import android.widget.*; import app.freeink.data.DocStore; import app.freeink.util.Ui; import java.util.*;

public class MainActivity extends Activity {
    DocStore store; ListView list; ArrayAdapter<String> adapter; List<DocStore.Doc> shown=new ArrayList<>();
    String filter="All"; String query=""; EditText search; LinearLayout chips; TextView empty;
    static final int REQ_PDF=41, REQ_IMG=42;
    protected void onCreate(Bundle b){ super.onCreate(b); store=new DocStore(this); build(); handleShareIntent(getIntent()); }
    protected void onResume(){ super.onResume(); if(store!=null){ store=new DocStore(this); refresh(); } }
    void build(){ LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Ui.bg(this)); root.setPadding(Ui.dp(this,18),Ui.dp(this,14),Ui.dp(this,18),0);
        LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles=new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(Ui.tv(this,"FreeInk",26,Ui.ink(this),true)); titles.addView(Ui.tv(this,"Freeform notes & whiteboard — free forever",13,Ui.sub(this),false));
        head.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        Button settings=Ui.btn(this,"Settings",Ui.card(this),Ui.ink(this)); settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class))); head.addView(settings);
        Button plus=Ui.btn(this,"+  New",Ui.ACCENT,0xFFFFFFFF); plus.setOnClickListener(v->createMenu()); head.addView(plus); root.addView(head);
        search=new EditText(this); search.setHint("Search notes, notebooks, text…"); search.setSingleLine(true); search.setTextColor(Ui.ink(this)); search.setHintTextColor(Ui.sub(this)); search.setBackground(Ui.round(this,Ui.card(this),12,Ui.line(this))); search.setPadding(Ui.dp(this,14),Ui.dp(this,10),Ui.dp(this,14),Ui.dp(this,10));
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,-2); sp.setMargins(0,Ui.dp(this,14),0,Ui.dp(this,10)); root.addView(search,sp);
        search.addTextChangedListener(new android.text.TextWatcher(){ public void afterTextChanged(android.text.Editable s){ query=s.toString().toLowerCase(); refresh(); } public void beforeTextChanged(CharSequence s,int a,int b2,int c){} public void onTextChanged(CharSequence s,int a,int b2,int c){} });
        chips=new LinearLayout(this); chips.setOrientation(LinearLayout.HORIZONTAL); root.addView(chips);
        LinearLayout actions=new LinearLayout(this); actions.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,6));
        Button impPdf=Ui.btn(this,"Import PDF",Ui.card(this),Ui.ink(this)); impPdf.setOnClickListener(v->pick("application/pdf",REQ_PDF));
        Button impImg=Ui.btn(this,"Import image",Ui.card(this),Ui.ink(this)); impImg.setOnClickListener(v->pick("image/*",REQ_IMG));
        Button folder=Ui.btn(this,"New folder",Ui.card(this),Ui.ink(this)); folder.setOnClickListener(v->newFolder());
        actions.addView(impPdf); actions.addView(impImg); actions.addView(folder); root.addView(actions);
        empty=Ui.tv(this,"No documents yet.\nTap “+ New” to create your first notebook, whiteboard or text note.",15,Ui.sub(this),false); empty.setGravity(Gravity.CENTER); empty.setPadding(0,Ui.dp(this,40),0,0);
        list=new ListView(this); list.setDivider(null); list.setBackgroundColor(Ui.bg(this)); root.addView(empty); root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
        list.setOnItemClickListener((p,v,pos,id)->open(shown.get(pos)));
        list.setOnItemLongClickListener((p,v,pos,id)->{ docMenu(shown.get(pos)); return true; });
        setContentView(root); refreshChips(); refresh(); }
    void refreshChips(){ chips.removeAllViews(); List<String> fs=new ArrayList<>(); fs.add("All"); fs.add("★ Favorites"); fs.add("Recent"); fs.addAll(store.folders()); if(fs.size()>1)fs.remove(1+2); // keep single All
        LinkedHashSet<String> uniq=new LinkedHashSet<>(fs); for(String f:uniq){ Button b=Ui.btn(this,f,f.equals(filter)?Ui.ACCENT:Ui.card(this),f.equals(filter)?0xFFFFFFFF:Ui.ink(this)); b.setOnClickListener(v->{filter=f; refreshChips(); refresh();}); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,Ui.dp(this,38)); lp.setMargins(0,0,Ui.dp(this,8),0); chips.addView(b,lp); } }
    void refresh(){ if(list==null)return; shown.clear(); List<String> labels=new ArrayList<>();
        for(DocStore.Doc d:store.docs()){ if(filter.equals("★ Favorites")&&!d.favorite)continue; if(!filter.equals("All")&&!filter.equals("★ Favorites")&&!filter.equals("Recent")&&!filter.equals(d.folder))continue;
            if(!query.isEmpty()&&!(d.title.toLowerCase().contains(query)||d.snippet.toLowerCase().contains(query)||d.folder.toLowerCase().contains(query)))continue; shown.add(d); }
        if(filter.equals("Recent")) while(shown.size()>12)shown.remove(shown.size()-1);
        for(DocStore.Doc d:shown) labels.add((d.favorite?"★ ":"")+"●  "+d.title+"\n      "+Ui.typeLabel(d.type)+"  ·  "+d.folder+"  ·  "+Ui.fmtDate(d.updated));
        adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels){ public View getView(int pos,View cv,ViewGroup par){ TextView t=(TextView)super.getView(pos,cv,par); t.setTextColor(Ui.ink(MainActivity.this)); t.setTextSize(15); t.setPadding(Ui.dp(MainActivity.this,16),Ui.dp(MainActivity.this,14),Ui.dp(MainActivity.this,16),Ui.dp(MainActivity.this,14)); t.setBackground(Ui.round(MainActivity.this,Ui.card(MainActivity.this),14,Ui.line(MainActivity.this))); return t; } };
        list.setAdapter(adapter); empty.setVisibility(shown.isEmpty()?View.VISIBLE:View.GONE); list.setVisibility(shown.isEmpty()?View.GONE:View.VISIBLE); }
    void pick(String mime,int req){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType(mime); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,req); }
    protected void onActivityResult(int req,int res,Intent data){ super.onActivityResult(req,res,data); if(res!=RESULT_OK||data==null||data.getData()==null)return; Uri u=data.getData();
        try{ if(req==REQ_PDF){ String id=store.importPdf(this,u,nameOf(u)); DocStore.Doc d=store.get(id); try{ app.freeink.pdf.PdfHelper h=new app.freeink.pdf.PdfHelper(store.pdfFile(id)); d.pdfPages=h.pages(); h.close(); }catch(Exception ignored){} store.saveIndex(); open(d); }
            else { String id=store.importImageAsDoc(this,u); open(store.get(id)); } refresh(); }catch(Exception e){ toast("Import failed: "+e.getMessage()); } }
    String nameOf(Uri u){ try(Cursor c=getContentResolver().query(u,null,null,null,null)){ if(c!=null&&c.moveToFirst()){ int i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if(i>=0)return c.getString(i); } }catch(Exception ignored){} return "Imported"; }
    void handleShareIntent(Intent in){ if(in!=null&&Intent.ACTION_SEND.equals(in.getAction())&&in.getParcelableExtra(Intent.EXTRA_STREAM)!=null){ Uri u=in.getParcelableExtra(Intent.EXTRA_STREAM); String t=in.getType();
        try{ if(t!=null&&t.equals("application/pdf")) store.importPdf(this,u,nameOf(u)); else store.importImageAsDoc(this,u); store=new DocStore(this); }catch(Exception ignored){} } }
    void createMenu(){ String[] items={"Notebook page (handwriting)","Infinite whiteboard","Text note","Import PDF…","Import image…"};
        new AlertDialog.Builder(this).setTitle("Create").setItems(items,(d,w)->{ switch(w){ case 0: promptTitle("note"); break; case 1: promptTitle("whiteboard"); break; case 2: promptTitle("text"); break; case 3: pick("application/pdf",REQ_PDF); break; case 4: pick("image/*",REQ_IMG); break; } }).show(); }
    void promptTitle(String type){ EditText e=new EditText(this); e.setHint("Title"); e.setText(type.equals("whiteboard")?"Untitled whiteboard":type.equals("text")?"Untitled text note":"Untitled notebook");
        new AlertDialog.Builder(this).setTitle("New "+Ui.typeLabel(type)).setView(e).setPositiveButton("Create",(d,w)->{ DocStore.Doc doc=store.create(e.getText().toString().trim().isEmpty()?"Untitled":e.getText().toString(),type,filter.equals("All")||filter.contains("★")||filter.equals("Recent")?"All":filter,Ui.ACCENT); open(doc); refresh(); }).setNegativeButton("Cancel",null).show(); }
    void newFolder(){ EditText e=new EditText(this); e.setHint("Folder name"); new AlertDialog.Builder(this).setTitle("New folder").setView(e).setPositiveButton("Create",(d,w)->{ String n=e.getText().toString().trim(); if(!n.isEmpty()){ store.folders().add(n); store.saveIndex(); filter=n; refreshChips(); refresh(); } }).setNegativeButton("Cancel",null).show(); }
    void open(DocStore.Doc d){ if(d==null)return; store.touch(d); Intent i=new Intent(this,d.type.equals("text")?TextNoteActivity.class:EditorActivity.class); i.putExtra("id",d.id); startActivity(i); }
    void docMenu(DocStore.Doc d){ String[] items={"Open","Rename",d.favorite?"Unfavorite":"★ Favorite","Duplicate","Move to folder…","Delete"};
        new AlertDialog.Builder(this).setTitle(d.title).setItems(items,(x,w)->{ switch(w){ case 0: open(d); break; case 1: rename(d); break; case 2: d.favorite=!d.favorite; store.saveIndex(); refresh(); break; case 3: store.duplicate(d); store=new DocStore(this); refresh(); break; case 4: move(d); break; case 5: new AlertDialog.Builder(this).setTitle("Delete document?").setPositiveButton("Delete",(a,b)->{store.delete(d); refresh();}).setNegativeButton("Cancel",null).show(); break; } }).show(); }
    void rename(DocStore.Doc d){ EditText e=new EditText(this); e.setText(d.title); new AlertDialog.Builder(this).setTitle("Rename").setView(e).setPositiveButton("Save",(a,b)->{ d.title=e.getText().toString(); store.touch(d); refresh(); }).setNegativeButton("Cancel",null).show(); }
    void move(DocStore.Doc d){ List<String> fs=store.folders(); new AlertDialog.Builder(this).setTitle("Move to…").setItems(fs.toArray(new String[0]),(a,b)->{ d.folder=fs.get(b); store.saveIndex(); refresh(); }).show(); }
    void toast(String s){ Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
}
