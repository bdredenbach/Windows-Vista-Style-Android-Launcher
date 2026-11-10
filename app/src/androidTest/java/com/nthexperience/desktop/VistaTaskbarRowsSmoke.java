package com.nthexperience.desktop;

import android.app.*;import android.content.*;import android.content.pm.ActivityInfo;import android.graphics.*;import android.os.*;import android.view.*;import android.widget.*;import org.json.*;import java.util.*;

/** Real swipe/drag regression for the owner's independently scrolling taskbar rows. */
public final class VistaTaskbarRowsSmoke extends ExtendedTaskbarSmoke {
    int checks;
    @Override public void onStart(){Bundle out=new Bundle();try{
        shell("wm size 1080x2400");store=new DesktopStore(getTargetContext());
        getTargetContext().getSharedPreferences(StorageAccess.PREFS,0).edit().putBoolean("seen",true).commit();store.usagePrefs.edit().putBoolean(DesktopStore.TASKBAR_USAGE,false).commit();
        store.prefs.edit().putInt("startup_mode",2).putBoolean("welcome_on_start",false).putBoolean("reduce_motion",true).putBoolean("taskbar_static",false).putBoolean("taskbar_expanded",true).remove(TaskbarRows.KEY).commit();
        getTargetContext().getSharedPreferences(InitializationState.PREFS,0).edit().putInt("flow_version",2).putInt("step",InitializationState.Step.DONE.ordinal()).commit();
        getTargetContext().getSharedPreferences(StartupExperience.SESSION,0).edit().putBoolean("seen",true).putBoolean("welcome_seen",true).putBoolean(StartupExperience.HOME_OFFERED,true).commit();
        home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=home.getWindow().getDecorView();settle();
        for(String alignment:new String[]{"Vista"}){
            ui(()->{closeAllWindows();for(String id:ExternalTasks.list())ExternalTasks.remove(id);store.prefs.edit().remove(TaskbarRows.KEY).putBoolean("taskbar_static",false).putBoolean("taskbar_expanded",true).commit();store.quickLaunch(Arrays.asList("@photos","@browser","@recycle","@calendar","@welcome","@sticky","@computer","@files","@documents","@notes","@backup","@system"));refreshBar();for(String id:new String[]{"@notes","@system","@recent","@backup","@taskmanager"})home.open(id);});settle();
            ui(()->{TaskbarItems items=find(TaskbarItems.class);items.scrollRowTo(0,0);items.scrollRowTo(1,0);check(items.rowRange(0)>Aero.dp(home,60)&&items.rowRange(1)>Aero.dp(home,60),"Both rows actually overflow");});settle();
            Map<String,String> fixed=new LinkedHashMap<>();for(Map.Entry<String,View> e:fixedViews().entrySet())fixed.put(e.getKey(),bounds(e.getValue()));
            List<String> top=geometryRow(0),bottom=geometryRow(1);swipeRow(0);waitFling();
            check(find(TaskbarItems.class).rowScroll(0)>0,"Upper swipe moves upper row");check(geometryRow(1).equals(bottom),"Upper swipe leaves every bottom app stationary");
            top=geometryRow(0);swipeRow(1);waitFling();check(find(TaskbarItems.class).rowScroll(1)>0,"Lower swipe moves lower row");check(geometryRow(0).equals(top),"Lower swipe leaves every upper app stationary");
            for(Map.Entry<String,View> e:fixedViews().entrySet())check(fixed.get(e.getKey()).equals(bounds(e.getValue())),"Fixed controls never scroll: "+e.getKey());
            ui(()->{TaskbarItems items=find(TaskbarItems.class);items.stopScrolling();items.scrollRowTo(0,0);items.scrollRowTo(1,0);store.prefs.edit().commit();refreshBar();});settle();
            dragAcross("quick:@photos",0);checkRow("quick:@photos",0,"Idle pin dragged to top");
            dragAcross("task:@notes",1);checkRow("task:@notes",1,"Running app dragged to bottom");
            tap(slot("quick:@photos"),60);await(()->window("@photos")!=null&&window("@photos").isShown(),"Moved Quick Launch shortcut opens correct app");
            String saved=store.prefs.getString(TaskbarRows.KEY,"");
            ui(()->{home.open("@photos");try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField("windows");f.setAccessible(true);Map<?,?> windows=(Map<?,?>)f.get(home);((DesktopWindow)windows.get("@notes")).closeWindow();}catch(Exception e){throw new RuntimeException(e);}});settle();
            checkRow("quick:@photos",0,"Pin retains chosen row after launch");ui(()->home.open("@notes"));settle();checkRow("task:@notes",1,"Running app retains chosen bottom row after reopen");
            ui(this::toggleExpanded);settle();check(!find(TaskbarItems.class).expanded,"Collapsed mode remains one strip");ui(this::toggleExpanded);settle();checkRow("quick:@photos",0,"Expand restores saved top choice");checkRow("task:@notes",1,"Expand restores saved bottom choice");
            ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));settle();checkRow("quick:@photos",0,"Top choice survives rotation");ui(()->home.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));settle();checkRow("task:@notes",1,"Bottom choice survives rotation");
            DesktopBackup.Plan plan=DesktopBackup.parse(DesktopBackup.snapshot(store).toString());check(saved.equals(plan.values.get(TaskbarRows.KEY)),"Portable backup retains row choices");
            ui(()->{try{store.prefs.edit().remove(TaskbarRows.KEY).commit();DesktopBackup.apply(home,store,plan,false);refreshBar();}catch(Exception e){throw new RuntimeException(e);}});settle();checkRow("quick:@photos",0,"Actual backup restore recovers top choice");checkRow("task:@notes",1,"Actual backup restore recovers bottom choice");
            ui(()->{store.prefs.edit().putBoolean("taskbar_static",true).commit();refreshBar();});settle();
            ui(()->{TaskbarItems items=find(TaskbarItems.class);for(int i=0;i<items.getChildCount();i++){View child=items.getChildAt(i);int row=TaskbarItems.row(child);check(child.getLeft()>=items.rowStart(row)&&child.getRight()<=items.rowEnd(row),"Static fit respects each manually assigned row");if(child instanceof FrameLayout)check(((FrameLayout)child).getChildAt(0).getWidth()<=child.getWidth(),"Fitted Quick Launch artwork is scaled inside its cell");}check(items.rowScroll(0)==0&&items.rowScroll(1)==0,"Static mode resets both scroll offsets");});
            shot("vista-rows-"+alignment.toLowerCase(Locale.ROOT)+".png");
        }
        ui(()->home.finish());waitForIdleSync();home=(MainActivity)startActivitySync(new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName(getTargetContext(),MainActivity.class)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));root=home.getWindow().getDecorView();store=new DesktopStore(home);settle();checkRow("quick:@photos",0,"Fresh Activity retains top choice");ui(()->home.open("@notes"));settle();checkRow("task:@notes",1,"Fresh Activity retains bottom choice");
        ui(()->{View notes=slot("task:@notes");check(notes.performAccessibilityAction(R.id.taskbar_move_top,null),"Accessible move-to-top action accepted");});settle();checkRow("task:@notes",0,"Accessibility row move persisted");
        ui(()->{View notes=slot("task:@notes");check(notes.requestFocusFromTouch(),"Task button receives keyboard focus");check(notes.dispatchKeyEvent(new KeyEvent(0,0,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN,0,KeyEvent.META_CTRL_ON|KeyEvent.META_SHIFT_ON)),"Keyboard row move accepted");});settle();checkRow("task:@notes",1,"Keyboard row move persisted");
        extraInputCases();
        out.putString("stream","PASS: "+checks+" independent-row assertions; real upper/lower swipes leave all apps in the opposite row stationary, native drag/drop in both directions, default and saved placement through launch/close/reopen, collapse/expand, rotation, portable backup, static fitting, fixed controls and accessible/keyboard row moves.\n");finish(Activity.RESULT_OK,out);
    }catch(Throwable e){out.putString("stream","FAIL: "+android.util.Log.getStackTraceString(e));try{shot("vista-rows-failure.png");}catch(Exception ignored){}finish(Activity.RESULT_CANCELED,out);}}
    @Override protected void check(boolean ok,String message){checks++;super.check(ok,message);}
    List<String> geometryRow(int row){List<String> result=new ArrayList<>();TaskbarItems items=find(TaskbarItems.class);for(int i=0;i<items.getChildCount();i++){View c=items.getChildAt(i);if(TaskbarItems.row(c)==row)result.add(TaskbarReorder.key(c)+":"+bounds(c));}return result;}
    void checkRow(String id,int row,String message)throws Exception{ui(()->{View child=slot(id);check(child!=null&&TaskbarItems.row(child)==row&&child.getTop()==row*Aero.dp(home,48),message);});}
    void touch(int action,float x,float y,long down){MotionEvent event=MotionEvent.obtain(down,SystemClock.uptimeMillis(),action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(event,true),"Native row input accepted");event.recycle();}
    void timedTouch(int action,float x,float y,long down,long time){MotionEvent event=MotionEvent.obtain(down,time,action,x,y,0);event.setSource(InputDevice.SOURCE_TOUCHSCREEN);check(getUiAutomation().injectInputEvent(event,true),"Timed native row swipe accepted");event.recycle();}
    void swipeRow(int row)throws Exception{TaskbarItems items=find(TaskbarItems.class);int[] at=new int[2];items.getLocationOnScreen(at);float start=at[0]+items.rowEnd(row)-Aero.dp(home,8),end=at[0]+items.rowStart(row)+Aero.dp(home,8),y=at[1]+Aero.dp(home,row*48+24);long now=SystemClock.uptimeMillis();timedTouch(MotionEvent.ACTION_DOWN,start,y,now,now);for(int i=1;i<=6;i++)timedTouch(MotionEvent.ACTION_MOVE,start+(end-start)*i/6,y,now,now+i*14);timedTouch(MotionEvent.ACTION_UP,end,y,now,now+98);}

    void waitFling()throws Exception{Thread.sleep(700);ui(()->find(TaskbarItems.class).stopScrolling());settle();}
    void dragAcross(String id,int row)throws Exception{
        ui(()->{TaskbarItems items=find(TaskbarItems.class);items.stopScrolling();items.scrollRowTo(0,0);items.scrollRowTo(1,0);});settle();
        View source=slot(id);int[] at=new int[2],origin=new int[2];source.getLocationOnScreen(origin);TaskbarItems items=find(TaskbarItems.class);items.getLocationOnScreen(at);float x=origin[0]+source.getWidth()/2f,y=origin[1]+source.getHeight()/2f,targetX=at[0]+items.rowStart(row)+Aero.dp(home,16),targetY=at[1]+Aero.dp(home,row*48+24);long down=SystemClock.uptimeMillis();touch(MotionEvent.ACTION_DOWN,x,y,down);Thread.sleep(185);touch(MotionEvent.ACTION_MOVE,x+Aero.dp(home,14),y,down);Thread.sleep(120);
        for(int i=1;i<=5;i++){touch(MotionEvent.ACTION_MOVE,x+(targetX-x)*i/5,y+(targetY-y)*i/5,down);Thread.sleep(35);}touch(MotionEvent.ACTION_UP,targetX,targetY,down);Thread.sleep(150);settle();
    }
    void refreshBar(){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod("renderTaskbar");m.setAccessible(true);m.invoke(home);}catch(Exception e){throw new RuntimeException(e);}}
    void toggleExpanded(){store.prefs.edit().putBoolean("taskbar_expanded",!store.prefs.getBoolean("taskbar_expanded",false)).commit();refreshBar();}
    void closeAllWindows(){try{java.lang.reflect.Field f=MainActivity.class.getDeclaredField("windows");f.setAccessible(true);for(Object w:new ArrayList<>(((Map<?,?>)f.get(home)).values()))((DesktopWindow)w).closeWindow();}catch(Exception e){throw new RuntimeException(e);}}
    void settle()throws Exception{idle();Thread.sleep(250);}
    <T extends View>T find(Class<T> type){return type.cast(find(v->type.isInstance(v)));}
    View slot(String key){return find(v->key.equals(TaskbarReorder.key(v)));}
    String bounds(View v){return screen(v).toShortString();}
    Map<String,View> fixedViews(){Map<String,View> result=new LinkedHashMap<>();for(String id:new String[]{"Start menu","Taskbar commands","Clock and calendar","Device controls","Show desktop"})result.put(id,find(v->id.equals(v.getContentDescription())));return result;}

    void extraInputCases()throws Exception{
        ui(()->{closeAllWindows();store.quickLaunch(Arrays.asList("@photos","@browser","@recycle","@calendar","@welcome","@sticky","@computer","@files","@documents","@notes","@backup","@system"));store.prefs.edit().remove(TaskbarRows.KEY).remove(TaskbarOrder.KEY).putBoolean("taskbar_static",false).commit();for(String id:new String[]{"@notes","@system","@recent","@backup","@taskmanager"})home.open(id);refreshBar();});settle();
        for(int row=0;row<2;row++){
            final int chosen=row;ui(()->{TaskbarItems items=find(TaskbarItems.class);items.scrollRowTo(0,0);items.scrollRowTo(1,0);});settle();List<String> other=geometryRow(1-row);
            TaskbarItems items=find(TaskbarItems.class);int[] at=new int[2];items.getLocationOnScreen(at);float x=at[0]+(items.rowStart(row)+items.rowEnd(row))/2f,y=at[1]+Aero.dp(home,row*48+24);long time=SystemClock.uptimeMillis();
            MotionEvent.PointerProperties prop=new MotionEvent.PointerProperties();prop.id=0;prop.toolType=MotionEvent.TOOL_TYPE_MOUSE;MotionEvent.PointerCoords coord=new MotionEvent.PointerCoords();coord.x=x;coord.y=y;coord.setAxisValue(MotionEvent.AXIS_VSCROLL,-3);
            MotionEvent wheel=MotionEvent.obtain(time,time,MotionEvent.ACTION_SCROLL,1,new MotionEvent.PointerProperties[]{prop},new MotionEvent.PointerCoords[]{coord},0,0,1,1,0,0,InputDevice.SOURCE_MOUSE,0);check(getUiAutomation().injectInputEvent(wheel,true),"Mouse wheel injected");wheel.recycle();settle();check(items.rowScroll(row)>0,"Mouse wheel moves selected row");check(other.equals(geometryRow(1-row)),"Mouse wheel leaves opposite row fixed");
        }
        ui(()->{TaskbarItems i=find(TaskbarItems.class);i.scrollRowTo(0,0);i.scrollRowTo(1,0);});settle();
        String rows=store.prefs.getString(TaskbarRows.KEY,"{}"),order=store.prefs.getString(TaskbarOrder.KEY,"[]");
        dragOutside("task:@notes");check(rows.equals(store.prefs.getString(TaskbarRows.KEY,"{}"))&&order.equals(store.prefs.getString(TaskbarOrder.KEY,"[]")),"Rejected drop never changes row or order");check(slot("task:@notes").getAlpha()==1f,"Rejected drop restores source opacity");
        // Hold at a row edge, move outside, and drop: scroll the hovered row only.
        ui(()->{TaskbarItems i=find(TaskbarItems.class);i.scrollRowTo(0,0);i.scrollRowTo(1,0);});settle();List<String> other=geometryRow(1);
        View source=slot("task:@notes");Rect sourceBounds=screen(source);TaskbarItems items=find(TaskbarItems.class);Rect area=screen(items);long down=SystemClock.uptimeMillis();float x=sourceBounds.exactCenterX(),y=sourceBounds.exactCenterY();
        touch(MotionEvent.ACTION_DOWN,x,y,down);Thread.sleep(185);touch(MotionEvent.ACTION_MOVE,x+Aero.dp(home,14),y,down);Thread.sleep(100);touch(MotionEvent.ACTION_MOVE,area.right-Aero.dp(home,2),y,down);await(()->items.rowScroll(0)>0,"Drag edge scroll moves hovered row: range="+items.rowRange(0)+", source alpha="+source.getAlpha()+", bounds="+area);check(other.equals(geometryRow(1)),"Drag edge scroll leaves opposite row fixed");
        touch(MotionEvent.ACTION_MOVE,area.exactCenterX(),area.top-Aero.dp(home,70),down);Thread.sleep(80);int stopped=items.rowScroll(0);Thread.sleep(100);check(items.rowScroll(0)==stopped,"Leaving row stops edge scroll");touch(MotionEvent.ACTION_UP,area.exactCenterX(),area.top-Aero.dp(home,70),down);settle();check(slot("task:@notes").getAlpha()==1f,"Interrupted edge drag restores opacity");
        ui(()->{closeAllWindows();store.quickLaunch(Collections.emptyList());store.prefs.edit().remove(TaskbarRows.KEY).remove(TaskbarOrder.KEY).commit();home.open("@notes");refreshBar();});settle();check(find(TaskbarItems.class).getChildCount()==1,"Single app fixture");dragAcross("task:@notes",1);checkRow("task:@notes",1,"Single app moves into empty bottom row");dragAcross("task:@notes",0);checkRow("task:@notes",0,"Single app moves into empty top row");
        // Stationary holds still use Vista's real system menu.
        tap(slot("task:@notes"),700);awaitNode("Close");shell("input keyevent 4");settle();
    }
    void dragOutside(String id)throws Exception{
        View source=slot(id);Rect r=screen(source),area=screen(find(TaskbarItems.class));float x=r.exactCenterX(),y=r.exactCenterY();long down=SystemClock.uptimeMillis();touch(MotionEvent.ACTION_DOWN,x,y,down);Thread.sleep(185);touch(MotionEvent.ACTION_MOVE,x+Aero.dp(home,14),y,down);Thread.sleep(100);touch(MotionEvent.ACTION_MOVE,area.exactCenterX(),area.top-Aero.dp(home,80),down);Thread.sleep(100);touch(MotionEvent.ACTION_UP,area.exactCenterX(),area.top-Aero.dp(home,80),down);settle();
    }

}
