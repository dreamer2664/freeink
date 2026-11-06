package app.freeink.util;
import android.content.*; import android.graphics.*; import android.graphics.drawable.GradientDrawable; import android.view.*; import android.widget.*; import app.freeink.data.DocStore;

public class Ui {
    public static boolean dark(Context c){ return c.getSharedPreferences("freeink",0).getBoolean("dark",false); }
    public static int bg(Context c){ return dark(c)?0xFF1C1C1E:0xFFF5F5F7; }
    public static int card(Context c){ return dark(c)?0xFF2C2C2E:0xFFFFFFFF; }
    public static int ink(Context c){ return dark(c)?0xFFF5F5F7:0xFF1D1D1F; }
    public static int sub(Context c){ return dark(c)?0xFF98989F:0xFF86868B; }
    public static int line(Context c){ return dark(c)?0xFF3A3A3C:0xFFE5E5EA; }
    public static final int ACCENT=0xFF0071E3;
    public static int dp(Context c,int v){ return (int)(v*c.getResources().getDisplayMetrics().density); }
    public static GradientDrawable round(Context c,int color,int radiusDp,int stroke){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(c,radiusDp)); if(stroke!=0)g.setStroke(dp(c,1),stroke); return g; }
    public static Button btn(Context c,String t,int bg,int fg){ Button b=new Button(c); b.setText(t); b.setAllCaps(false); b.setTextSize(14); b.setTextColor(fg); b.setBackground(round(c,bg,10,0)); b.setPadding(dp(c,14),0,dp(c,14),0); return b; }
    public static TextView tv(Context c,String t,float sp,int color,boolean bold){ TextView v=new TextView(c); v.setText(t); v.setTextSize(sp); v.setTextColor(color); if(bold)v.setTypeface(Typeface.DEFAULT_BOLD); return v; }
    public static String typeLabel(String t){ switch(t){case "whiteboard":return "Whiteboard";case "pdf":return "PDF";case "text":return "Text note";default:return "Notebook page";} }
    public static String fmtDate(long t){ return new java.text.SimpleDateFormat("MMM d, HH:mm",java.util.Locale.getDefault()).format(new java.util.Date(t)); }
}
