package com.nthexperience.desktop;
import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import org.json.*;

/** Native regression for the recorded desktop reflow and task button insertion failures. */
public final class ExtendedTaskbarSmoke extends Instrumentation {
    private MainActivity home;private View root;private DesktopStore store;
    private final BlockingQueue<MainActivity> resumed=new LinkedBlockingQueue<>();
    private final Application.ActivityLifecycleCallbacks lifecycle=new Application.ActivityLifecycleCallbacks(){public void onActivityResumed(Activity a){if(a instanceof MainActivity)resumed.offer((MainActivity)a);}public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}public void onActivityStopped(Activity a){}public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityPaused(Activity a){}public void onActivityDestroyed(Activity a){}};
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();boolean success=false;Application app=(Application)getTargetContext().getApplicationContext();app.registerActivityLifecycleCallbacks(lifecycle);try{
        store=new DesktopStore(getTargetContext());store.prefs.edit().clear().putInt("startup_mode",2).putBoolean("welcome_on_start",false).putBoolean("reduce_motion",true).putBoolean("sidebar_visible",false).putBoolean("tray_expanded",false).putString("taskbar_grouping","Never").putInt("desktop_rows_portrait",8).putInt("desktop_columns_portrait",4).putInt("desktop_rows_landscape",4).putInt("desktop_columns_landscape",8).commit();
        getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).commit();getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).putBoolean("welcome_seen",true).putBoolean(StartupExperience.HOME_OFFERED,true).commit();
        store.usagePrefs.edit().putBoolean(DesktopStore.TASKBAR_USAGE,false).commit();
        home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=home.getWindow().getDecorView();await(()->home.hasWindowFocus(),"Desktop focus");
        await(()->find(v->v instanceof DesktopIconGrid)!=null&&((DesktopIconGrid)find(v->v instanceof DesktopIconGrid)).getChildCount()>0,"Desktop ready");
        ui(()->home.open("@notes"));
        for(boolean snap:new boolean[]{true,false}){
            store.prefs.edit().putBoolean("snap_desktop",snap).commit();
            for(int orientation:new int[]{ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}){
                ui(()->home.setRequestedOrientation(orientation));await(()->orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT?root.getHeight()>root.getWidth():root.getWidth()>root.getHeight(),"Orientation settled");idle();
                if(!snap)ui(()->{DesktopIconGrid grid=grid();grid.place(String.valueOf(grid.getChildAt(0).getTag()),37,79);});idle();
                Map<String,Rect> before=icons();String saved=store.prefs.getString(orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT?"desktop_positions_portrait":"desktop_positions_landscape","{}");int oldHeight=barView().getHeight();
                for(int repeat=0;repeat<2;repeat++){
                    bar("Expand taskbar");await(()->barView().getHeight()==oldHeight+Aero.dp(home,48),"Extra row has exact height");idle();check(before.equals(icons()),"Every desktop icon keeps its pixel bounds after expansion");check(saved.equals(store.prefs.getString(orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT?"desktop_positions_portrait":"desktop_positions_landscape","{}")),"Expansion never rewrites icon positions");
                    check(strip().getHeight()==Aero.dp(home,96),"Two full-size rows");check(((ScrollView)grid().getParent()).canScrollVertically(1),"Desktop area covered by extra row is scrollable");if(snap&&orientation==ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE&&repeat==0)shot("vista-taskbar-two-rows-landscape.png");
                    bar("Expand taskbar");await(()->barView().getHeight()==oldHeight,"Normal height restored");idle();check(before.equals(icons()),"Collapse keeps every desktop icon fixed");
                }
            }
        }
        ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));await(()->root.getHeight()>root.getWidth(),"Portrait restored");
        AppCatalog.Entry fixture=null;for(AppCatalog.Entry entry:AppCatalog.load(home))if(entry.id.startsWith("com.nthexperience.desktop.fixture/"))fixture=entry;check(fixture!=null,"External app fixture exists");String fixtureId=fixture.id;
        ui(()->{ExternalTasks.opened(fixtureId);home.open("@calendar");});idle();List<String> original=tasks();check(original.contains(fixtureId),"Android button rendered");
        ui(()->home.open("setting:page:taskbar"));await(()->find(v->v instanceof CheckBox&&"Expand taskbar to two rows".contentEquals(((CheckBox)v).getText()))!=null,"Control Panel option exists");idle();List<String> appended=new ArrayList<>(original);appended.add("@control");check(tasks().equals(appended),"Opening Control Panel appends its button after existing Android tasks");
        CheckBox box=(CheckBox)find(v->v instanceof CheckBox&&"Expand taskbar to two rows".contentEquals(((CheckBox)v).getText()));tap(box,60);await(()->store.prefs.getBoolean("taskbar_expanded",false)&&strip().getHeight()==Aero.dp(home,96),"Control Panel expands taskbar immediately");
        bar("Expand taskbar");await(()->!box.isChecked()&&!store.prefs.getBoolean("taskbar_expanded",true),"Menu synchronizes the open Control Panel checkbox");bar("Expand taskbar");
        for(String id:new String[]{"@backup","@system","@sticky"})ui(()->home.open(id));idle();
        for(String mode:new String[]{"Static","Scrollable"}){bar(mode);idle();ViewGroup buttons=(ViewGroup)strip().getChildAt(0);boolean second=false;for(int i=0;i<buttons.getChildCount();i++){View button=buttons.getChildAt(i);second|=button.getTop()>0;check(button.getWidth()>0&&button.getHeight()==Aero.dp(home,48),"Task buttons have nonzero width and normal height");if(mode.equals("Static"))check(button.getRight()<=strip().getWidth(),"Static task buttons fit the viewport");}check(second,"Extra row contains app buttons");if(mode.equals("Static"))check(!strip().canScrollHorizontally(1),"Static mode does not scroll");else check(strip().canScrollHorizontally(1),"Scrollable overflow remains reachable");}
        check(Boolean.TRUE.equals(DesktopBackup.parse(DesktopBackup.snapshot(store).toString()).values.get("taskbar_expanded")),"Backup preserves expanded setting");
        List<String> order=tasks();shot("vista-taskbar-two-rows.png");resumed.clear();MainActivity old=home;ui(home::recreate);MainActivity next=null;long until=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<until){MainActivity a=resumed.poll(300,TimeUnit.MILLISECONDS);if(a!=null&&a!=old){next=a;break;}}check(next!=null,"Activity recreation completed");home=next;root=home.getWindow().getDecorView();await(()->home.hasWindowFocus()&&tasks().equals(order),"Activity recreation restores task order");check(strip().getHeight()==Aero.dp(home,96),"Activity recreation remembers the extra row");
        tap(find(v->"Start menu".equals(v.getContentDescription())),60);check(find(v->v instanceof StartMenu)!=null,"Start menu opens above expanded taskbar");shot("vista-taskbar-two-rows-start.png");tap(find(v->"Start menu".equals(v.getContentDescription())),60);
        result.putString("stream","PASS: two taskbar controls; repeated expansion/collapse with fixed pixel icon geometry in portrait/landscape and snapped/free layouts; stable insertion after external tasks; two full-size rows, Static fitting and Scrollable overflow; backup and Activity recreation; Start menu.\n");success=true;
    }catch(Throwable failure){try{shot("vista-taskbar-failure.png");StringBuilder tree=new StringBuilder();dump(getUiAutomation().getRootInActiveWindow(),tree,0);try(FileOutputStream out=getTargetContext().openFileOutput("vista-taskbar-tree.txt",0)){out.write(tree.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}}catch(Exception ignored){}StringWriter text=new StringWriter();failure.printStackTrace(new PrintWriter(text));result.putString("stream","FAIL: "+text);
    }finally{app.unregisterActivityLifecycleCallbacks(lifecycle);}finish(success?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
    private void idle()throws Exception{getUiAutomation().waitForIdle(250,4000);waitForIdleSync();Thread.sleep(180);}
    private DesktopIconGrid grid(){return (DesktopIconGrid)find(v->v instanceof DesktopIconGrid);}
    private View barView(){return find(v->"Desktop taskbar".equals(v.getContentDescription()));}
    private HorizontalScrollView strip(){return (HorizontalScrollView)find(v->"taskbar-items".equals(v.getTag()));}
    private Map<String,Rect> icons(){Map<String,Rect> out=new LinkedHashMap<>();DesktopIconGrid grid=grid();for(int i=0;i<grid.getChildCount();i++){View icon=grid.getChildAt(i);out.put(String.valueOf(icon.getTag()),new Rect(icon.getLeft(),icon.getTop(),icon.getRight(),icon.getBottom()));}return out;}
    private List<String> tasks(){List<String> out=new ArrayList<>();HorizontalScrollView strip=strip();if(strip==null)return out;ViewGroup items=(ViewGroup)strip.getChildAt(0);for(int i=0;i<items.getChildCount();i++)if(items.getChildAt(i).getTag() instanceof String)out.add((String)items.getChildAt(i).getTag());return out;}
    private void bar(String command)throws Exception{tap(find(v->"Taskbar commands".equals(v.getContentDescription())),700);awaitNode("Start Task Manager");menuTap(command);}
    private DesktopWindow window(String id){return (DesktopWindow)findAll(root,v->v instanceof DesktopWindow&&id.equals(((DesktopWindow)v).toolId));}
    private void close(String id)throws Exception{DesktopWindow w=window(id);if(w!=null){if(!w.isShown())ui(()->home.open(id));tap(find(w,v->"Close window".equals(v.getContentDescription())),60);}}
    private View text(View base,String text){return find(base,v->v instanceof TextView&&text.contentEquals(((TextView)v).getText()));}
    private View find(Predicate<View> match){return find(root,match);}private View find(View v,Predicate<View> match){if(v==null||!v.isShown())return null;if(match.test(v))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View result=find(((ViewGroup)v).getChildAt(i),match);if(result!=null)return result;}return null;}
    private View findAll(View v,Predicate<View> match){if(v==null)return null;if(match.test(v))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View result=findAll(((ViewGroup)v).getChildAt(i),match);if(result!=null)return result;}return null;}
    private void tap(View v,long hold)throws Exception{check(v!=null,"Native touch target exists");await(()->home.hasWindowFocus(),"Focused native target");ui(()->v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true));getUiAutomation().waitForIdle(150,2500);Rect r=new Rect();check(v.isEnabled()&&v.getGlobalVisibleRect(r)&&!r.isEmpty(),"Native target has visible bounds: "+v.getContentDescription());long t=SystemClock.uptimeMillis();input(t,MotionEvent.ACTION_DOWN,r.exactCenterX(),r.exactCenterY());Thread.sleep(hold);input(t,MotionEvent.ACTION_UP,r.exactCenterX(),r.exactCenterY());Thread.sleep(100);}
    private void doubleTap(View v)throws Exception{Rect r=new Rect();check(v.getGlobalVisibleRect(r),"Caption has visible bounds");for(int i=0;i<2;i++){long t=SystemClock.uptimeMillis();input(t,MotionEvent.ACTION_DOWN,r.exactCenterX(),r.exactCenterY());Thread.sleep(35);input(t,MotionEvent.ACTION_UP,r.exactCenterX(),r.exactCenterY());Thread.sleep(60);}}
    private void input(long down,int action,float x,float y){MotionEvent e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(e,true),"Native input injected");e.recycle();}
    private void menuTap(String text)throws Exception{awaitNode(text);getUiAutomation().waitForIdle(150,2500);AccessibilityNodeInfo node=node(text);check(node!=null&&node.isEnabled(),"Enabled menu command: "+text);Rect r=new Rect();node.getBoundsInScreen(r);long t=SystemClock.uptimeMillis();input(t,MotionEvent.ACTION_DOWN,r.exactCenterX(),r.exactCenterY());Thread.sleep(55);input(t,MotionEvent.ACTION_UP,r.exactCenterX(),r.exactCenterY());Thread.sleep(150);}
    private void awaitNode(String text)throws Exception{long end=SystemClock.uptimeMillis()+8000;while(SystemClock.uptimeMillis()<end){if(node(text)!=null)return;Thread.sleep(100);}throw new AssertionError("Native menu node: "+text);}
    private AccessibilityNodeInfo node(String text){return node(getUiAutomation().getRootInActiveWindow(),text);}private AccessibilityNodeInfo node(AccessibilityNodeInfo n,String text){if(n==null)return null;if(text.contentEquals(n.getText()==null?"":n.getText())||text.contentEquals(n.getContentDescription()==null?"":n.getContentDescription()))return n;for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo r=node(n.getChild(i),text);if(r!=null)return r;}return null;}
    private void ui(Runnable r){runOnMainSync(r);try{Thread.sleep(80);}catch(InterruptedException e){throw new AssertionError(e);}}
    private void await(BooleanSupplier condition,String text)throws Exception{long end=SystemClock.uptimeMillis()+12000;while(SystemClock.uptimeMillis()<end){boolean[] ready={false};runOnMainSync(()->ready[0]=condition.getAsBoolean());if(ready[0])return;Thread.sleep(100);}throw new AssertionError(text);}
    private String shell(String command)throws Exception{try(ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);return out.toString("UTF-8");}}
    private void shot(String name)throws Exception{getUiAutomation().waitForIdle(150,2500);Bitmap image=getUiAutomation().takeScreenshot();check(image!=null,"Screenshot available");try(FileOutputStream out=getTargetContext().openFileOutput(name,0)){image.compress(Bitmap.CompressFormat.PNG,100,out);}image.recycle();}
    private void dump(AccessibilityNodeInfo n,StringBuilder out,int depth){if(n==null||depth>24)return;out.append(depth).append(' ').append(n.getText()).append(" / ").append(n.getContentDescription()).append('\n');for(int i=0;i<n.getChildCount();i++)dump(n.getChild(i),out,depth+1);}
    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
}
