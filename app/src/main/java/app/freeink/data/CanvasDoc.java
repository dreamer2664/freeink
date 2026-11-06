package app.freeink.data;
import org.json.*;
import java.util.*;

public class CanvasDoc {
    public static class Pt { public float x,y,p; public Pt(float x,float y,float p){this.x=x;this.y=y;this.p=p;} }
    public static class Stroke { public int color; public float width; public boolean highlighter; public List<Pt> pts=new ArrayList<>();
        JSONObject toJson() throws JSONException { JSONObject o=new JSONObject(); o.put("c",color); o.put("w",width); o.put("h",highlighter);
            JSONArray a=new JSONArray(); for(Pt pt:pts){a.put(pt.x);a.put(pt.y);a.put(pt.p);} o.put("p",a); return o; }
        static Stroke fromJson(JSONObject o) throws JSONException { Stroke s=new Stroke(); s.color=o.optInt("c",0xFF111111); s.width=(float)o.optDouble("w",4); s.highlighter=o.optBoolean("h");
            JSONArray a=o.getJSONArray("p"); for(int i=0;i+2<a.length();i+=3) s.pts.add(new Pt((float)a.getDouble(i),(float)a.getDouble(i+1),(float)a.getDouble(i+2))); return s; } }
    public static class TextObj { public float x,y,size=42; public int color=0xFF111111; public String text="Text"; public boolean bold;
        JSONObject toJson() throws JSONException { JSONObject o=new JSONObject(); o.put("x",x);o.put("y",y);o.put("s",size);o.put("c",color);o.put("t",text);o.put("b",bold); return o; }
        static TextObj fromJson(JSONObject o){ TextObj t=new TextObj(); t.x=(float)o.optDouble("x");t.y=(float)o.optDouble("y");t.size=(float)o.optDouble("s",42);t.color=o.optInt("c",0xFF111111);t.text=o.optString("t","Text");t.bold=o.optBoolean("b"); return t; } }
    public static class ShapeObj { public int kind; //0 rect 1 ellipse 2 line 3 arrow 4 triangle
        public float x,y,x2,y2; public int color; public float width=4; public boolean fill;
        JSONObject toJson() throws JSONException { JSONObject o=new JSONObject(); o.put("k",kind);o.put("x",x);o.put("y",y);o.put("x2",x2);o.put("y2",y2);o.put("c",color);o.put("w",width);o.put("f",fill); return o; }
        static ShapeObj fromJson(JSONObject o){ ShapeObj s=new ShapeObj(); s.kind=o.optInt("k");s.x=(float)o.optDouble("x");s.y=(float)o.optDouble("y");s.x2=(float)o.optDouble("x2");s.y2=(float)o.optDouble("y2");s.color=o.optInt("c",0xFF111111);s.width=(float)o.optDouble("w",4);s.fill=o.optBoolean("f"); return s; } }
    public static class ImageObj { public String file; public float x,y,w=420,h=300,rot;
        JSONObject toJson() throws JSONException { JSONObject o=new JSONObject(); o.put("f",file);o.put("x",x);o.put("y",y);o.put("w",w);o.put("h",h);o.put("r",rot); return o; }
        static ImageObj fromJson(JSONObject o){ ImageObj im=new ImageObj(); im.file=o.optString("f");im.x=(float)o.optDouble("x");im.y=(float)o.optDouble("y");im.w=(float)o.optDouble("w",420);im.h=(float)o.optDouble("h",300);im.rot=(float)o.optDouble("r"); return im; } }
    public static class Page { public List<Stroke> strokes=new ArrayList<>(); public List<TextObj> texts=new ArrayList<>(); public List<ShapeObj> shapes=new ArrayList<>(); public List<ImageObj> images=new ArrayList<>();
        JSONObject toJson() throws JSONException { JSONObject o=new JSONObject(); JSONArray a=new JSONArray(); for(Stroke s:strokes)a.put(s.toJson()); o.put("strokes",a);
            a=new JSONArray(); for(TextObj t:texts)a.put(t.toJson()); o.put("texts",a); a=new JSONArray(); for(ShapeObj s:shapes)a.put(s.toJson()); o.put("shapes",a);
            a=new JSONArray(); for(ImageObj i:images)a.put(i.toJson()); o.put("images",a); return o; }
        public static Page fromJsonSafe(Page src){ try{ return fromJson(src.toJson()); }catch(Exception e){ return new Page(); } }
        static Page fromJson(JSONObject o) throws JSONException { Page p=new Page(); JSONArray a=o.optJSONArray("strokes"); if(a!=null)for(int i=0;i<a.length();i++)p.strokes.add(Stroke.fromJson(a.getJSONObject(i)));
            a=o.optJSONArray("texts"); if(a!=null)for(int i=0;i<a.length();i++)p.texts.add(TextObj.fromJson(a.getJSONObject(i)));
            a=o.optJSONArray("shapes"); if(a!=null)for(int i=0;i<a.length();i++)p.shapes.add(ShapeObj.fromJson(a.getJSONObject(i)));
            a=o.optJSONArray("images"); if(a!=null)for(int i=0;i<a.length();i++)p.images.add(ImageObj.fromJson(a.getJSONObject(i))); return p; } }

    public String template="blank"; // blank|ruled|dotted|grid|dark
    public List<Page> pages=new ArrayList<>();
    public CanvasDoc(){ pages.add(new Page()); }
    public String toJson(){ try{ JSONObject o=new JSONObject(); o.put("template",template); JSONArray a=new JSONArray(); for(Page p:pages)a.put(p.toJson()); o.put("pages",a); return o.toString(); }catch(Exception e){ return "{\"pages\":[]}"; } }
    public static CanvasDoc fromJson(String s){ CanvasDoc d=new CanvasDoc(); try{ JSONObject o=new JSONObject(s); d.template=o.optString("template","blank"); d.pages.clear();
        JSONArray a=o.optJSONArray("pages"); if(a!=null&&a.length()>0) for(int i=0;i<a.length();i++) d.pages.add(Page.fromJson(a.getJSONObject(i))); if(d.pages.isEmpty()) d.pages.add(new Page());
    }catch(Exception ignored){} return d; }
    public String plainText(){ StringBuilder b=new StringBuilder(); for(Page p:pages) for(TextObj t:p.texts) b.append(t.text).append(' '); return b.toString(); }
}
