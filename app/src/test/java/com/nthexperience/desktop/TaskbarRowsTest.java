package com.nthexperience.desktop;
import android.content.Context;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=35,manifest=Config.NONE)
public class TaskbarRowsTest {
    DesktopStore store;Context context;
    @Before public void setup(){context=RuntimeEnvironment.getApplication();store=new DesktopStore(context);store.prefs.edit().clear().commit();}
    @Test public void duplicateShortcutAndTaskKeepIndependentRows()throws Exception{
        TaskbarRows.save(store.prefs,"quick:@notes",0);TaskbarRows.save(store.prefs,"task:@notes",1);
        assertEquals(Integer.valueOf(0),TaskbarRows.read(store.prefs).get("quick:@notes"));assertEquals(Integer.valueOf(1),TaskbarRows.read(store.prefs).get("task:@notes"));
        DesktopBackup.Plan plan=DesktopBackup.parse(DesktopBackup.snapshot(store).toString());store.prefs.edit().remove(TaskbarRows.KEY).commit();DesktopBackup.apply(context,store,plan,false);
        assertEquals(Integer.valueOf(1),TaskbarRows.read(store.prefs).get("task:@notes"));
    }
    @Test public void malformedRowsAreRejectedBeforeRestore()throws Exception{
        for(String raw:Arrays.asList("{\"task:@notes\":2}","{\"task:@notes\":-1}","{\"task:@notes\":\"0\"}","{\"task:@notes\":true}","{\"task:@notes\":0.5}","{\"\":0}","{\"a\\n\":0}")){
            JSONObject backup=DesktopBackup.snapshot(store);backup.getJSONObject("settings").put(TaskbarRows.KEY,new JSONObject().put("type","string").put("value",raw));
            try{DesktopBackup.parse(backup.toString());fail(raw);}catch(JSONException expected){}
            assertFalse(store.prefs.contains(TaskbarRows.KEY));
        }
    }
    @Test public void boundedMapsRejectOversizeAndSaveEvictsOldest()throws Exception{
        JSONObject tooMany=new JSONObject();for(int i=0;i<=TaskbarOrder.LIMIT;i++){tooMany.put("task:app"+i,0);TaskbarRows.save(store.prefs,"task:app"+i,i%2);}
        try{TaskbarRows.parse(tooMany.toString());fail();}catch(JSONException expected){}
        assertEquals(TaskbarOrder.LIMIT,TaskbarRows.read(store.prefs).size());assertFalse(TaskbarRows.read(store.prefs).containsKey("task:app0"));
    }
    @Test public void oldBackupsDoNotRequireRows()throws Exception{
        JSONObject old=DesktopBackup.snapshot(store);old.getJSONObject("settings").remove(TaskbarRows.KEY);old.getJSONObject("settings").remove(TaskbarOrder.KEY);
        DesktopBackup.Plan plan=DesktopBackup.parse(old.toString());assertFalse(plan.values.containsKey(TaskbarRows.KEY));
    }
    @Test public void savedOrderRetainsUnavailableTasksUntilTheyReturn(){
        List<String> saved=Arrays.asList("task:@notes","quick:@notes","task:@calendar");
        assertEquals(Arrays.asList("quick:@notes","task:@calendar"),TaskbarOrder.apply(Arrays.asList("task:@calendar","quick:@notes"),saved));
        assertEquals(saved,TaskbarOrder.apply(Arrays.asList("task:@calendar","quick:@notes","task:@notes"),saved));
    }
}
