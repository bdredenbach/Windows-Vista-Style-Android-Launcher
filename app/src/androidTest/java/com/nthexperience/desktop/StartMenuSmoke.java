package com.nthexperience.desktop;

import android.app.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

/** Real touch input regression: ListView callbacks alone missed the intercepted-row bug. */
public final class StartMenuSmoke extends Instrumentation {
    private MainActivity home;private View root;private DesktopStore store;private int assertions;
    private static final String APP="com.nthexperience.desktop.fixture/com.nthexperience.desktop.fixture.TaskActivity";
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){Bundle result=new Bundle();try{
        store=new DesktopStore(getTargetContext());store.prefs.edit().putInt("startup_mode",2).putBoolean("welcome_on_start",false).putBoolean("reduce_motion",true).putBoolean("desktop_sounds",false).commit();
        getTargetContext().getSharedPreferences(InitializationState.PREFS,0).edit().clear().putInt("flow_version",2).putInt("step",InitializationState.Step.DONE.ordinal()).commit();
        getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).commit();
        getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).putBoolean("welcome_seen",true).commit();
        store.startPins(Arrays.asList(APP,"@notes"));
        home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=home.getWindow().getDecorView();
        await(()->uiValue(()->home.hasWindowFocus()),"Desktop focused");
        for(int orientation:new int[]{ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE}){
            ui(()->home.setRequestedOrientation(orientation));await(()->uiValue(()->orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT?root.getHeight()>root.getWidth():root.getWidth()>root.getHeight()),"Orientation");
            show();tapProgram(APP,70);await(()->accessible("Task probe instance"),"Pinned Android app opens by actual tap");backHome();
            show();tap(find(v->v instanceof TextView&&"All Programs  ›".contentEquals(((TextView)v).getText())),70);tapProgram(APP,70);await(()->accessible("Task probe instance"),"All Programs Android app opens by actual tap");backHome();
            show();ui(()->search().setText("Task switch probe"));tapProgram(APP,70);await(()->accessible("Task probe instance"),"Search Android app opens by actual tap");backHome();
            show();tapProgram("@notes",70);await(()->find(v->"Notepad text".equals(v.getContentDescription()))!=null,"Built-in program opens by actual tap");
            show();tapProgram(APP,850);await(()->accessible("Vista Start item menu"),"Stationary hold opens Vista menu");
            for(String title:Arrays.asList("Open","Run as administrator","Open file location","Unpin from Start Menu","Add to Quick Launch","Send To","Cut","Copy","Delete","Rename","Properties","Uninstall"))check(accessible(title),"Vista program command "+title);
            check(!accessible("Task probe instance"),"Hold does not launch Android app");shot("vista-start-menu-"+(orientation==ActivityInfo.SCREEN_ORIENTATION_PORTRAIT?"portrait":"landscape")+".png");
            clickAccessible("Open");await(()->accessible("Task probe instance"),"Menu Open launches app");backHome();
        }
        show();mouse(program(APP));await(()->accessible("Vista Start item menu"),"Mouse secondary click opens Vista menu");check(!accessible("Task probe instance"),"Mouse right-click does not launch");dismiss();
        tapProgram(APP,850);clickAccessible("Send To");await(()->accessible("Desktop (create shortcut)"),"Real Send To submenu");clickAccessible("Desktop (create shortcut)");check(store.desktop().contains(APP),"Send To creates desktop shortcut");
        tapProgram(APP,850);clickAccessible("Unpin from Start Menu");await(()->!store.startPins().contains(APP),"Unpin modifies Start only");check(store.desktop().contains(APP),"Unpin preserves desktop shortcut");
        tapProgram(APP,850);clickAccessible("Remove from this list");await(()->program(APP)==null,"Remove from this list persists on landing");
        ui(()->search().setText("Task switch probe"));tapProgram(APP,70);await(()->accessible("Task probe instance"),"Removed recent app remains available in search");backHome();
        show();tap(find(v->"Documents".equals(v.getContentDescription())),850);await(()->accessible("Vista Documents menu"),"Navigation hold opens appropriate menu");dismiss();
        tap(find(v->v instanceof TextView&&"All Programs  ›".contentEquals(((TextView)v).getText())),70);tapProgram("start-group:Accessories",850);await(()->accessible("Vista Start item menu"),"Program group hold menu");clickAccessible("Open");await(()->program("@notes")!=null,"Group Open navigates Accessories");tapProgram("@notes",70);await(()->find(v->"Notepad text".equals(v.getContentDescription()))!=null,"Accessories item opens");
        result.putString("stream","PASS: "+assertions+" Start menu assertions; actual touchscreen taps launch Android apps from pinned, All Programs and search in portrait/landscape; built-in and Accessories taps; stationary holds and menu Open; Vista command layout, Send To desktop, Start-only unpin, recent removal with search access, navigation/group menus.\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable error){try{shot("vista-start-failure.png");}catch(Exception ignored){}StringWriter out=new StringWriter();error.printStackTrace(new PrintWriter(out));result.putString("stream","FAIL: "+out);finish(Activity.RESULT_CANCELED,result);}}
    private void show()throws Exception{await(()->find(v->"Start menu".equals(v.getContentDescription())&&v.getWidth()>0)!=null,"Start button ready");if(find(v->v instanceof StartMenu&&v.getVisibility()==View.VISIBLE)==null)tap(find(v->"Start menu".equals(v.getContentDescription())),70);await(()->find(v->v instanceof StartMenu&&v.getVisibility()==View.VISIBLE)!=null,"Start visible");}
    private EditText search(){StartMenu menu=(StartMenu)find(v->v instanceof StartMenu&&v.getVisibility()==View.VISIBLE);return (EditText)findIn(menu,v->v instanceof EditText);}
    private View program(String id){return uiValue(()->{
        StartMenu menu=(StartMenu)findIn(root,v->v instanceof StartMenu&&v.getVisibility()==View.VISIBLE);if(menu==null)return null;
        ListView list=(ListView)findIn(menu,v->v instanceof ListView);if(list==null)return null;
        for(int i=0;i<list.getChildCount();i++){int p=list.getFirstVisiblePosition()+i;StartSearch.Result item=(StartSearch.Result)list.getAdapter().getItem(p);if(item.id.equals(id))return list.getChildAt(i);}return null;
    });}
    private void backHome()throws Exception{
        // The emulator's default HOME is its system launcher. Bring this test launcher
        // back explicitly after verifying the separate Android app really opened.
        try(InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(getUiAutomation().executeShellCommand("am start -W -a android.intent.action.MAIN -n com.nthexperience.desktop/.MainActivity"))){byte[] bytes=new byte[1024];while(in.read(bytes)!=-1){}}
        await(()->uiValue(()->home.hasWindowFocus()),"Back to desktop");
    }
    private void back(){long now=SystemClock.uptimeMillis();getUiAutomation().injectInputEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK,0),true);getUiAutomation().injectInputEvent(new KeyEvent(now,SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK,0),true);}
    private void dismiss()throws Exception{back();waitForIdleSync();await(()->!accessible("Vista Start item menu")&&!accessible("Vista Documents menu"),"Context menu dismissed");getUiAutomation().waitForIdle(150,2500);}
    private void tapProgram(String id,long hold)throws Exception{
        await(()->uiValue(()->{StartMenu menu=(StartMenu)findIn(root,v->v instanceof StartMenu);if(menu==null)return false;ListView list=(ListView)findIn(menu,v->v instanceof ListView);for(int i=0;i<list.getAdapter().getCount();i++)if(((StartSearch.Result)list.getAdapter().getItem(i)).id.equals(id)){list.setSelection(i);return true;}return false;}),"Program available: "+id);
        waitForIdleSync();await(()->program(id)!=null,"Program row ready: "+id);tap(program(id),hold);
    }
    private void tap(View view,long hold)throws Exception{check(view!=null,"Touch target exists");getUiAutomation().waitForIdle(150,2500);int[] xy=new int[2];ui(()->view.getLocationOnScreen(xy));float x=xy[0]+view.getWidth()/2f,y=xy[1]+view.getHeight()/2f;long down=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(down,down,MotionEvent.ACTION_DOWN,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(e,true),"Touch DOWN delivered");e.recycle();Thread.sleep(hold);e=MotionEvent.obtain(down,SystemClock.uptimeMillis(),MotionEvent.ACTION_UP,x,y,0);e.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(e,true),"Touch UP delivered");e.recycle();waitForIdleSync();getUiAutomation().waitForIdle(150,2500);}
    private void mouse(View view)throws Exception{
        check(view!=null,"Mouse target exists");int[] xy=new int[2];ui(()->view.getLocationOnScreen(xy));long down=SystemClock.uptimeMillis();
        MotionEvent.PointerProperties prop=new MotionEvent.PointerProperties();prop.id=0;prop.toolType=MotionEvent.TOOL_TYPE_MOUSE;
        MotionEvent.PointerCoords point=new MotionEvent.PointerCoords();point.x=xy[0]+view.getWidth()/2f;point.y=xy[1]+view.getHeight()/2f;point.pressure=1;point.size=1;
        for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,1,new MotionEvent.PointerProperties[]{prop},new MotionEvent.PointerCoords[]{point},0,action==MotionEvent.ACTION_DOWN?MotionEvent.BUTTON_SECONDARY:0,1,1,0,0,InputDevice.SOURCE_MOUSE,0);check(getUiAutomation().injectInputEvent(event,true),"Mouse button delivered");event.recycle();}waitForIdleSync();
    }
    private View find(Predicate<View> predicate){return uiValue(()->findIn(root,predicate));}
    private View findIn(View view,Predicate<View> predicate){if(view==null||view.getVisibility()!=View.VISIBLE)return null;if(predicate.test(view))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View found=findIn(((ViewGroup)view).getChildAt(i),predicate);if(found!=null)return found;}return null;}
    private AccessibilityNodeInfo node(AccessibilityNodeInfo node,String label){if(node==null)return null;if(label.contentEquals(node.getContentDescription()==null?"":node.getContentDescription())||label.contentEquals(node.getText()==null?"":node.getText()))return node;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo match=node(node.getChild(i),label);if(match!=null)return match;}return null;}
    private boolean accessible(String label){return node(getUiAutomation().getRootInActiveWindow(),label)!=null;}
    private void clickAccessible(String label)throws Exception{AccessibilityNodeInfo match=node(getUiAutomation().getRootInActiveWindow(),label);check(match!=null,"Menu target "+label);while(!match.isClickable()&&match.getParent()!=null)match=match.getParent();check(match.performAction(AccessibilityNodeInfo.ACTION_CLICK),"Menu action "+label);waitForIdleSync();}
    private void await(BooleanSupplier condition,String label)throws Exception{long until=SystemClock.uptimeMillis()+15000;while(SystemClock.uptimeMillis()<until){if(condition.getAsBoolean()){assertions++;return;}Thread.sleep(80);}throw new AssertionError(label);}
    private void check(boolean value,String label){if(!value)throw new AssertionError(label);assertions++;}
    private void ui(Runnable work){runOnMainSync(work);}
    private <T>T uiValue(Supplier<T> work){if(Looper.myLooper()==Looper.getMainLooper())return work.get();AtomicReference<T> result=new AtomicReference<>();runOnMainSync(()->result.set(work.get()));return result.get();}
    private void shot(String name)throws Exception{android.graphics.Bitmap image=getUiAutomation().takeScreenshot();if(image!=null)try(FileOutputStream out=getTargetContext().openFileOutput(name,0)){image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);image.recycle();}}
}
