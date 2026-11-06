package app.freeink.ui;
import android.app.Activity; import android.content.SharedPreferences; import android.os.Bundle; import android.widget.*; import app.freeink.util.Ui;

public class SettingsActivity extends Activity {
    protected void onCreate(Bundle b){ super.onCreate(b); SharedPreferences p=getSharedPreferences("freeink",0);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Ui.bg(this)); root.setPadding(Ui.dp(this,20),Ui.dp(this,16),Ui.dp(this,20),0);
        root.addView(Ui.tv(this,"Settings",26,Ui.ink(this),true));
        CheckBox dark=new CheckBox(this); dark.setText("Dark mode"); dark.setTextColor(Ui.ink(this)); dark.setChecked(p.getBoolean("dark",false)); dark.setOnCheckedChangeListener((v,on)->p.edit().putBoolean("dark",on).apply()); root.addView(dark);
        CheckBox finger=new CheckBox(this); finger.setText("Allow finger drawing (off = palm rejection when stylus is used)"); finger.setTextColor(Ui.ink(this)); finger.setChecked(p.getBoolean("finger",true)); finger.setOnCheckedChangeListener((v,on)->p.edit().putBoolean("finger",on).apply()); root.addView(finger);
        String[] templates={"blank","ruled","dotted","grid","dark"}; 
        root.addView(Ui.tv(this,"Default paper template",15,Ui.ink(this),true));
        Spinner sp=new Spinner(this); sp.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,templates));
        String cur=p.getString("template","blank"); for(int i=0;i<templates.length;i++) if(templates[i].equals(cur)) sp.setSelection(i);
        sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){ public void onItemSelected(AdapterView<?> a,android.view.View v,int pos,long id){ p.edit().putString("template",templates[pos]).apply(); } public void onNothingSelected(AdapterView<?> a){} }); root.addView(sp);
        root.addView(Ui.tv(this,"\nPen thickness (1–12)",15,Ui.ink(this),true));
        SeekBar sb=new SeekBar(this); sb.setMax(11); sb.setProgress(Math.max(0,(int)p.getFloat("penWidth",4)-1)); sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){ public void onProgressChanged(SeekBar s,int v,boolean u){ p.edit().putFloat("penWidth",v+1).apply(); } public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){} }); root.addView(sb);
        TextView about=Ui.tv(this,"\nFreeInk 1.0.0\n100% free & open source (MIT). No ads, no accounts, no tracking.\nAll documents stay on this device.\n\nLibraries: none — Android framework only.\nPDF rendering: Android PdfRenderer · Export: Android PdfDocument.",14,Ui.sub(this),false); root.addView(about);
        setContentView(root); }
}
