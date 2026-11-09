package com.nthexperience.desktop;
import android.content.Context;
import android.view.*;
import android.widget.TextView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
public class WelcomeNavigationTest {
    @Test public void startHasExplicitWelcomeCenterAction(){
        Context context=RuntimeEnvironment.getApplication();DesktopStore store=new DesktopStore(context);store.prefs.edit().clear().commit();AeroWallpaper wallpaper=new AeroWallpaper(context,11);String[] opened={null};StartMenu menu=new StartMenu(context,new StartMenu.Actions(){public void open(String id){opened[0]=id;}public void options(View view,String id){}public void home(){}public void settings(){}public boolean isHome(){return true;}},store,wallpaper);
        try{View welcome=find(menu);assertNotNull("Welcome Center is directly discoverable in Start",welcome);welcome.performClick();assertEquals("@welcome",opened[0]);}finally{menu.hide();wallpaper.close();}
    }
    private View find(View view){if(view instanceof TextView&&"Welcome Center".contentEquals(((TextView)view).getText()))return view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){View result=find(((ViewGroup)view).getChildAt(i));if(result!=null)return result;}return null;}
}
