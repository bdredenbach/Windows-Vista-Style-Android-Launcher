package com.nthexperience.desktop;

import android.content.*;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import java.util.*;
import java.util.function.Consumer;

/** Vista Start shortcut menus. Editing a Start shortcut never removes its app or desktop icon. */
final class StartItemMenus {
    static boolean hidden(DesktopStore store,String id){return values(store,"start_deleted").contains(id);}
    static boolean recentHidden(DesktopStore store,String id){return values(store,"start_removed_recent").contains(id);}
    private static List<String> values(DesktopStore store,String key){
        List<String> ids=new ArrayList<>();
        try{JSONArray array=new JSONArray(store.prefs.getString(key,"[]"));for(int i=0;i<array.length();i++)ids.add(array.getString(i));}catch(org.json.JSONException ignored){}
        return ids;
    }
    private static void mark(DesktopStore store,String key,String id){List<String> ids=values(store,key);if(!ids.contains(id))ids.add(id);store.prefs.edit().putString(key,new JSONArray(ids).toString()).apply();}
    static DesktopContextMenu create(Context context,View anchor,DesktopStore store,StartSearch.Result item,
            boolean landing,Consumer<String> open,Runnable changed,Consumer<StartSearch.Result> activate){
        String id=item.id;
        boolean group=id.startsWith("start-group:"),pinned=store.startPins().contains(id);
        boolean shortcut=item.shortcut||id.equals("@notes")||id.equals("@sticky")||id.startsWith("@gadget:");
        boolean program=shortcut&&!store.isFolder(id)&&!FolderLinks.id(id);
        DesktopContextMenu menu=new DesktopContextMenu(context,"Vista Start item menu",30,246);
        menu.item("Open","empty",()->activate.accept(item));menu.defaultLast();
        if(group){
            menu.item("Explore","files",()->activate.accept(item));
            menu.separator();
            menu.item("Properties","empty",()->Aero.dialog(context).setTitle(item.title+" Properties").setMessage("Start Menu program folder\n"+item.title).setPositiveButton("OK",null).show());
            return menu;
        }
        if(program){
            menu.item("Run as administrator","security",false,()->{});
            menu.item("Open file location","empty",false,()->{});
        }
        if(shortcut){
            menu.separator();
            menu.item(pinned?"Unpin from Start Menu":"Pin to Start Menu","empty",pinned||store.startPins().size()<32,()->{
                List<String> pins=store.startPins();if(pinned)pins.remove(id);else pins.add(id);store.startPins(pins);changed.run();
            });
            menu.item("Add to Quick Launch","empty",!store.quickLaunch().contains(id)&&store.quickLaunch().size()<DesktopStore.MAX_QUICK,()->{
                List<String> quick=store.quickLaunch();quick.add(id);store.quickLaunch(quick);changed.run();
            });
            menu.item("Restore previous versions","empty",false,()->{});
            if(landing&&!pinned)menu.item("Remove from this list","empty",()->{mark(store,"start_removed_recent",id);changed.run();});
            menu.separator();
            menu.submenu("Send To","empty",()->{
                DesktopContextMenu send=new DesktopContextMenu(context,"Vista Send To menu",30,246);
                send.item("Desktop (create shortcut)","computer",()->{if(!store.pinned(id))store.toggle(id);changed.run();});
                ContextMenus.centered(anchor,(x,y)->ContextMenus.show(send,anchor,x,y));
            });
            menu.separator();
            menu.item("Cut","cut",pinned,()->{ExplorerClipboard.shortcuts(Collections.singletonList(id),"start",true);changed.run();});
            menu.item("Copy","copy",()->ExplorerClipboard.shortcuts(Collections.singletonList(id),"start",false));
            menu.item("Delete","delete",()->Aero.dialog(context).setTitle("Delete Shortcut").setMessage("Remove “"+item.title+"” from the Start Menu? The app and its other shortcuts will be kept.")
                .setPositiveButton("Delete",(d,w)->{List<String> pins=store.startPins();pins.remove(id);store.startPins(pins);mark(store,"start_deleted",id);changed.run();}).setNegativeButton("Cancel",null).show());
            menu.item("Rename","empty",()->{
                EditText name=new EditText(context);name.setSingleLine(true);name.setText(item.title);name.selectAll();
                Aero.dialog(context).setTitle("Rename Shortcut").setView(name).setPositiveButton("OK",(d,w)->{
                    try{store.rename(id,name.getText().toString());changed.run();}catch(IllegalArgumentException invalid){Toast.makeText(context,invalid.getMessage(),Toast.LENGTH_LONG).show();}
                }).setNegativeButton("Cancel",null).show();
            });
        }
        menu.separator();
        menu.item("Properties","empty",()->properties(context,store,item));
        // Retain the explicitly requested Android uninstall integration as a separate command.
        if(AppUninstaller.available(context,id)){menu.separator();AppUninstaller.add(menu,context,id);}
        return menu;
    }
    private static void properties(Context context,DesktopStore store,StartSearch.Result item){
        ComponentName component=ComponentName.unflattenFromString(item.id);
        if(component!=null){
            try{context.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+component.getPackageName())));}
            catch(ActivityNotFoundException|SecurityException error){Toast.makeText(context,"App properties are unavailable.",Toast.LENGTH_SHORT).show();}
            return;
        }
        String detail=store.isFolder(item.id)?"App folder\n"+store.folderItems(item.id).size()+" shortcuts":FolderLinks.id(item.id)?"Folder shortcut":"Nth Desktop item";
        if(item.shortcut||item.id.startsWith("@"))detail+="\n\nRun as administrator and Windows program-file locations do not apply to Android apps or built-in tools.";
        Aero.dialog(context).setTitle(item.title+" Properties").setMessage(detail).setPositiveButton("OK",null).show();
    }
}
