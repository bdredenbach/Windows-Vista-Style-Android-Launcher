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
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

/** Real Settings return paths, optional setup and the existing Welcome Center retry route. */
public final class VistaInitializationSmoke extends Instrumentation {
    private MainActivity home;
    private View root;
    private DesktopStore store;
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            store=new DesktopStore(getTargetContext());
            store.prefs.edit().clear().putInt("startup_mode",2).putBoolean("welcome_on_start",false)
                    .putBoolean("reduce_motion",true).putBoolean("sidebar_visible",false)
                    .putString("weather_city","Saved city").commit();
            getTargetContext().getSharedPreferences(InitializationState.PREFS,0).edit().clear().commit();
            getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().clear().commit();
            getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit().clear()
                    .putBoolean("seen",true).putBoolean("welcome_seen",false).commit();
            home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN)
                    .setComponent(new ComponentName(getTargetContext(),MainActivity.class))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            root=home.getWindow().getDecorView();
            for(String title:new String[]{"Default Launcher","Set up Nth Explorer","Set up Usage Access",
                    "Allow Notifications Access","Modify System Settings (Brightness)","Allow Location"}){
                awaitSystem(title);
                check(find(root,WelcomeCenter.class)!=null,"Vista Welcome Center stays behind setup");
                if(title.equals("Set up Usage Access"))orderedUsage();
                systemClick("Later");
            }
            await(()->new InitializationState(home).step()==InitializationState.Step.DONE,"All optional steps finish");
            check(!StorageAccess.has(home)&&!ExternalTasks.permitted(home),"Later leaves optional grants off");
            check("Saved city".equals(store.prefs.getString("weather_city","")),"Saved weather choice survives setup");
            recreateHome();await(()->home.hasWindowFocus(),"Completed Home regains focus");
            check(new InitializationState(home).step()==InitializationState.Step.DONE,"Completed setup stays complete");
            manualUsage();
            grantedUsage();
            result.putString("stream","PASS: Vista-branded Welcome backdrop and scrollable ordered guides; actual Android Usage Access return without a grant retains the same guide, including repeated attempts and recreation; Android App info opens this app and returns to the same guide on API33+; portrait/landscape controls fit; Later explicitly continues; completed setup and saved city persist; Welcome Center manual retry survives Settings, App info and recreation; actual Android AppOps grant advances setup and checks the taskbar option.\n");
            finish(Activity.RESULT_OK,result);
        }catch(Throwable failure){
            try{shot("vista-usage-failure");dumpTree();}catch(Exception ignored){}
            result.putString("stream","FAIL: "+android.util.Log.getStackTraceString(failure));
            finish(Activity.RESULT_CANCELED,result);
        }
    }
    private void orderedUsage()throws Exception{
        for(int attempt=0;attempt<2;attempt++){
            systemClick("OK");awaitPackage("com.android.settings");
            check(!ExternalTasks.permitted(home),"Usage grant is still off in Settings");
            back();awaitSystem("Set up Usage Access");
            check(new InitializationState(home).step()==InitializationState.Step.USAGE,"Back without a grant preserves Usage Access");
            check(new InitializationState(home).waiting()==0,"Returned request markers clear");
        }
        shot("vista-usage-return-portrait");recreateHome();awaitSystem("Set up Usage Access");
        check(new InitializationState(home).step()==InitializationState.Step.USAGE,"Denied return survives recreation");
        appInfoReturn();
        ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        await(()->root.getWidth()>root.getHeight(),"Landscape guide settles");awaitSystem("Set up Usage Access");
        controlsFit();shot("vista-usage-return-landscape");
        ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        await(()->root.getHeight()>root.getWidth(),"Portrait guide settles");awaitSystem("Set up Usage Access");
        controlsFit();shot("vista-usage-return-after-recreation");
    }
    private void appInfoReturn()throws Exception{
        if(Build.VERSION.SDK_INT<33)return;
        systemClick("Open App info");awaitPackage("com.android.settings");
        awaitSystem(home.getString(R.string.app_name));shot("vista-usage-android-app-info");
        back();awaitSystem("Set up Usage Access");
        check(!ExternalTasks.permitted(home),"App info return does not manufacture a grant");
        Thread.sleep(500);awaitPackage(home.getPackageName());
        check(node(getUiAutomation().getRootInActiveWindow(),"Open App info")!=null,"Recovery stays available after App info");
    }
    private void manualUsage()throws Exception{
        WelcomeCenter welcome=find(root,WelcomeCenter.class);check(welcome!=null,"Existing Welcome Center retry route exists");
        View link=text(welcome,"Set up Usage Access");check(link!=null,"Welcome Center has Set up Usage Access");
        ui(()->link.requestRectangleOnScreen(new Rect(0,0,link.getWidth(),link.getHeight()),true));
        systemClick("Set up Usage Access");awaitSystem("OK");
        systemClick("OK");awaitPackage("com.android.settings");back();awaitSystem("Set up Usage Access");
        check(new InitializationState(home).step()==InitializationState.Step.DONE,"Manual retry keeps completed setup");
        appInfoReturn();recreateHome();awaitSystem("Set up Usage Access");
        check(new InitializationState(home).step()==InitializationState.Step.DONE,"Manual guide recreation keeps completion");
        shot("vista-usage-manual-return");systemClick("Cancel");
    }
    private void grantedUsage()throws Exception{
        // Android's real AppOps service provides the grant; Activity result codes are not used as permission proof.
        ui(()->getTargetContext().getSharedPreferences(InitializationState.PREFS,0).edit()
                .putInt("step",InitializationState.Step.USAGE.ordinal()).commit());
        recreateHome();awaitSystem("Set up Usage Access");systemClick("OK");awaitPackage("com.android.settings");
        shell("appops set --uid "+home.getPackageName()+" GET_USAGE_STATS allow");
        check(ExternalTasks.permitted(home),"Android's actual Usage Access grant is visible");
        back();awaitSystem("Allow Notifications Access");
        check(new InitializationState(home).step()==InitializationState.Step.NOTIFICATIONS,"Real grant advances initialization");
        for(String title:new String[]{"Allow Notifications Access","Modify System Settings (Brightness)","Allow Location"}){
            awaitSystem(title);systemClick("Later");
        }
        await(()->new InitializationState(home).step()==InitializationState.Step.DONE,"Granted setup completes");
        ui(()->home.open("setting:page:taskbar"));
        await(()->{View view=text(root,"Include apps opened outside Nth Desktop");return view instanceof CheckBox&&((CheckBox)view).isChecked();},"Real permission refreshes taskbar checkbox");
    }
    private void controlsFit()throws Exception{
        for(String label:Build.VERSION.SDK_INT>=33?new String[]{"OK","Later","Open App info"}:new String[]{"OK","Later"}){
            Rect r=new Rect();awaitSystem(label).getBoundsInScreen(r);
            check(!r.isEmpty()&&r.left>=0&&r.top>=0&&r.right<=root.getWidth()&&r.bottom<=root.getHeight(),"Reachable guide control: "+label);
        }
    }
    private void recreateHome()throws Exception{
        MainActivity previous=home;BlockingQueue<MainActivity> next=new LinkedBlockingQueue<>();
        Application app=(Application)getTargetContext().getApplicationContext();
        Application.ActivityLifecycleCallbacks callback=new Application.ActivityLifecycleCallbacks(){
            public void onActivityResumed(Activity a){if(a instanceof MainActivity&&a!=previous)next.offer((MainActivity)a);}
            public void onActivityCreated(Activity a,Bundle b){}public void onActivityStarted(Activity a){}
            public void onActivityStopped(Activity a){}public void onActivityPaused(Activity a){}
            public void onActivitySaveInstanceState(Activity a,Bundle b){}public void onActivityDestroyed(Activity a){}
        };
        app.registerActivityLifecycleCallbacks(callback);
        try{ui(home::recreate);home=next.poll(20,TimeUnit.SECONDS);check(home!=null,"Home recreated");root=home.getWindow().getDecorView();}
        finally{app.unregisterActivityLifecycleCallbacks(callback);}
    }
    private void awaitPackage(String pkg)throws Exception{
        long until=SystemClock.uptimeMillis()+15000;
        while(SystemClock.uptimeMillis()<until){AccessibilityNodeInfo n=getUiAutomation().getRootInActiveWindow();if(n!=null&&pkg.contentEquals(n.getPackageName()))return;Thread.sleep(80);}
        throw new AssertionError("Expected Android window: "+pkg);
    }
    private AccessibilityNodeInfo awaitSystem(String label)throws Exception{
        long until=SystemClock.uptimeMillis()+12000;
        while(SystemClock.uptimeMillis()<until){AccessibilityNodeInfo found=node(getUiAutomation().getRootInActiveWindow(),label);if(found!=null)return found;Thread.sleep(80);}
        throw new AssertionError("Missing native control: "+label);
    }
    private AccessibilityNodeInfo node(AccessibilityNodeInfo n,String label){
        if(n==null)return null;
        if(label.equalsIgnoreCase(String.valueOf(n.getText()))||label.equalsIgnoreCase(String.valueOf(n.getContentDescription())))return n;
        for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo found=node(n.getChild(i),label);if(found!=null)return found;}
        return null;
    }
    private void systemClick(String label)throws Exception{
        AccessibilityNodeInfo n=awaitSystem(label);Rect r=new Rect();n.getBoundsInScreen(r);
        for(int i=0;n!=null&&i<6;i++,n=n.getParent())if(n.isClickable()&&n.performAction(AccessibilityNodeInfo.ACTION_CLICK)){waitForIdleSync();Thread.sleep(180);return;}
        long now=SystemClock.uptimeMillis();MotionEvent down=MotionEvent.obtain(now,now,0,r.exactCenterX(),r.exactCenterY(),0),up=MotionEvent.obtain(now,now+60,1,r.exactCenterX(),r.exactCenterY(),0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try{check(getUiAutomation().injectInputEvent(down,true)&&getUiAutomation().injectInputEvent(up,true),"Native click: "+label);}finally{down.recycle();up.recycle();}
        waitForIdleSync();Thread.sleep(180);
    }
    private void back(){check(getUiAutomation().performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK),"Android Back");}
    private void ui(Runnable run)throws Exception{FutureTask<Void> task=new FutureTask<>(run,null);runOnMainSync(task);task.get(20,TimeUnit.SECONDS);}
    private void await(BooleanSupplier condition,String why)throws Exception{
        long until=SystemClock.uptimeMillis()+15000;
        while(SystemClock.uptimeMillis()<until){FutureTask<Boolean> task=new FutureTask<>(condition::getAsBoolean);runOnMainSync(task);if(task.get(10,TimeUnit.SECONDS))return;Thread.sleep(80);}
        throw new AssertionError(why);
    }
    private <T extends View>T find(View v,Class<T> type){if(type.isInstance(v))return type.cast(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){T found=find(((ViewGroup)v).getChildAt(i),type);if(found!=null)return found;}return null;}
    private View text(View v,String label){if(v instanceof TextView&&label.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View found=text(((ViewGroup)v).getChildAt(i),label);if(found!=null)return found;}return null;}
    private void shot(String name)throws Exception{
        waitForIdleSync();Thread.sleep(200);Bitmap b=getUiAutomation().takeScreenshot();check(b!=null,"Native screenshot available");
        try(OutputStream out=new FileOutputStream(new File(getTargetContext().getFilesDir(),name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}finally{b.recycle();}
    }
    private void dumpTree()throws Exception{StringBuilder out=new StringBuilder();dump(getUiAutomation().getRootInActiveWindow(),out,0);try(OutputStream file=getTargetContext().openFileOutput("vista-usage-tree.txt",0)){file.write(out.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));}}
    private void dump(AccessibilityNodeInfo n,StringBuilder out,int depth){if(n==null||depth>30)return;out.append(depth).append(' ').append(n.getText()).append(" / ").append(n.getContentDescription()).append('\n');for(int i=0;i<n.getChildCount();i++)dump(n.getChild(i),out,depth+1);}
    private String shell(String command)throws Exception{try(ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] bytes=new byte[4096];for(int n;(n=in.read(bytes))!=-1;)out.write(bytes,0,n);return out.toString("UTF-8");}}
    private static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
}
