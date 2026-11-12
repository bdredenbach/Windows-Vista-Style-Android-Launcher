package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.Color;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.util.*;

/** Vista's white program pane, dark navigation pane and bottom Start Search field. */
final class StartMenu extends LinearLayout {
    interface Actions{void open(String id);void options(View anchor,String id);void home();void settings();boolean isHome();default void shortcutsChanged(){}}
    private final EditText search;
    private final ListView list;
    private final Actions actions;
    private final DesktopStore store;
    private final LinearLayout profile,navigation,searchLinks;
    private final TextView programs,home;private TextView layoutLock;
    private final List<AppCatalog.Entry> all=new ArrayList<>();
    private final List<StartSearch.Result> filtered=new ArrayList<>();
    private final ListAdapter adapter=new ListAdapter();
    private boolean allPrograms;
    private String programFolder="";private DesktopContextMenu flyout;private int pinnedCount;
    private View heldOption;
    private int pendingListPosition=-1;
    StartMenu(Context c,Actions actions,DesktopStore store,AeroWallpaper wallpaper){
        super(c);this.actions=actions;this.store=store;setOrientation(HORIZONTAL);setElevation(Aero.dp(c,22));setPadding(Aero.dp(c,7),Aero.dp(c,7),Aero.dp(c,6),Aero.dp(c,7));setBackground(new AeroGlass(wallpaper,AeroGlass.START));
        LinearLayout left=Aero.column(c);left.setBackground(Aero.panel(c,0xfffdfefe,0xfff6f9fb,4,0xff8b9eaa));
        FrameLayout content=new FrameLayout(c);list=new ListView(c);list.setDividerHeight(0);list.setSelector(Aero.selection(c));list.setAdapter(adapter);list.setContentDescription("Installed programs. Hold an app for desktop options.");
        list.setOnItemClickListener((parent,view,position,id)->activate(filtered.get(position)));list.setOnItemLongClickListener((parent,view,position,id)->{itemMenu(view,filtered.get(position));return true;});content.addView(list,new FrameLayout.LayoutParams(-1,-1));
        TextView empty=Aero.text(c,"No matching results",12,0xff4b6170);empty.setGravity(Gravity.CENTER);empty.setVisibility(GONE);content.addView(empty,new FrameLayout.LayoutParams(-1,-1));list.setEmptyView(empty);left.addView(content,new LayoutParams(-1,0,1));
        searchLinks=Aero.column(c);for(String label:new String[]{"Search Everywhere","Search the Internet"}){TextView link=Aero.text(c,"⌕  "+label,11,0xff245d91);link.setPadding(Aero.dp(c,8),0,Aero.dp(c,3),0);link.setGravity(Gravity.CENTER_VERTICAL);link.setBackground(Aero.selection(c));link.setContentDescription(label);link.setOnClickListener(v->openSearch(label));searchLinks.addView(link,new LayoutParams(-1,Aero.dp(c,25)));}searchLinks.setVisibility(GONE);left.addView(searchLinks,new LayoutParams(-1,-2));
        programs=Aero.text(c,"All Programs  ›",12,Aero.INK);programs.setPadding(Aero.dp(c,10),0,Aero.dp(c,6),0);programs.setBackground(Aero.selection(c));programs.setOnClickListener(v->{if(!programFolder.isEmpty()){programFolder="";}else allPrograms=!allPrograms;programs.setText(allPrograms?"‹  Back":"All Programs  ›");filter();});left.addView(programs,new LayoutParams(-1,Aero.dp(c,32)));
        FrameLayout searchBox=new FrameLayout(c);searchBox.setPadding(Aero.dp(c,6),Aero.dp(c,4),Aero.dp(c,6),Aero.dp(c,5));searchBox.setBackgroundColor(0xffe5eaf0);
        search=new EditText(c);search.setTypeface(Aero.face(c));search.setSingleLine(true);search.setTextSize(12);search.setTextColor(Aero.INK);search.setHintTextColor(0xff657480);search.setHint("Start search");search.setContentDescription("Search apps, settings, notes and linked files");search.setPadding(Aero.dp(c,7),0,Aero.dp(c,22),0);search.setBackground(Aero.panel(c,0xffffffff,0xfff5f9fc,1,0xff728798));searchBox.addView(search,new FrameLayout.LayoutParams(-1,-1));TextView clear=Aero.text(c,"×",18,0xff496576);clear.setGravity(Gravity.CENTER);clear.setContentDescription("Clear Start search");clear.setOnClickListener(v->search.setText(""));searchBox.addView(clear,new FrameLayout.LayoutParams(Aero.dp(c,27),-1,Gravity.END));left.addView(searchBox,new LayoutParams(-1,Aero.dp(c,44)));
        addView(left,new LayoutParams(0,-1,.61f));
        // The parent supplies the glass once, so its opacity also reaches this pane.
        LinearLayout right=Aero.column(c);right.setContentDescription("Start navigation pane");right.setPadding(Aero.dp(c,7),0,0,0);setClipChildren(false);right.setClipChildren(false);addView(right,new LayoutParams(0,-1,.39f));
        profile=Aero.column(c);profile.setGravity(Gravity.CENTER);View avatar=ProfilePortrait.view(c,store);avatar.setContentDescription("Change profile picture");avatar.setFocusable(true);avatar.setOnClickListener(v->actions.open("setting:page:accounts"));ContextMenus.bind(avatar,(x,y)->navigationMenu(avatar,"setting:page:accounts","User account"));profile.addView(avatar,new LayoutParams(Aero.dp(c,49),Aero.dp(c,49)));TextView name=Aero.text(c,store.prefs.getString("desktop_name","Nth Desktop Vista"),13,Color.WHITE);name.setGravity(Gravity.CENTER);name.setShadowLayer(2,0,1,0xff000000);name.setContentDescription("Open user folder");name.setOnClickListener(v->actions.open("@files"));hoverOption(name,true);ContextMenus.bind(name,(x,y)->navigationMenu(name,"@files","User folder"));profile.addView(name,new LayoutParams(-1,Aero.dp(c,27)));right.addView(profile,new LayoutParams(-1,Aero.dp(c,86)));
        navigation=Aero.column(c);String[][] items={{"Documents","@documents"},{"Pictures","@pictures"},{"Music","@music"},{"Games","start-group:Games"},{"Recent Items  ›","start-recent"},{"Computer","@computer"},{"Network","setting:android:network"},{"Connect To","setting:android:wifi"},{"Control Panel","@control"},{"Default Programs","setting:android:defaults"},{"Welcome Center","@welcome"},{"Gadgets","@gadgets"}};
        for(String[] item:items){TextView nav=Aero.text(c,item[0],12,Color.WHITE);nav.setSingleLine(true);nav.setEllipsize(TextUtils.TruncateAt.END);nav.setPadding(Aero.dp(c,7),0,0,0);nav.setShadowLayer(2,0,1,0xff000000);nav.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(0x5571b6dc),Aero.panel(c,0x00ffffff,0x00ffffff,3,0),null));nav.setOnClickListener(v->{if(item[1].equals("start-recent"))recentMenu(nav);else if(item[1].startsWith("start-group:")){allPrograms=true;programFolder="Games";programs.setText("‹  Back");filter();}else actions.open(item[1]);});nav.setContentDescription(item[0]);hoverOption(nav,true);ContextMenus.bind(nav,(x,y)->navigationMenu(nav,item[1],item[0].replace("  ›","")));if(item[0].equals("Computer")||item[0].equals("Control Panel")){View line=new View(c);line.setBackgroundColor(0x668d999e);LayoutParams rule=new LayoutParams(-1,Aero.dp(c,1));rule.setMargins(Aero.dp(c,5),Aero.dp(c,4),Aero.dp(c,5),Aero.dp(c,4));navigation.addView(line,rule);}navigation.addView(nav,new LayoutParams(-1,Aero.dp(c,34)));if(item[1].equals("start-recent")){nav.setOnLongClickListener(v->{recentMenu(nav);return true;});nav.setOnHoverListener((v,event)->{if(event.getActionMasked()==MotionEvent.ACTION_HOVER_ENTER)recentMenu(nav);return false;});}}
        ScrollView navScroll=new ScrollView(c);navScroll.setVerticalScrollBarEnabled(false);navScroll.addView(navigation);right.addView(navScroll,new LayoutParams(-1,0,1));
        LinearLayout power=new LinearLayout(c);power.setGravity(Gravity.CENTER_VERTICAL);power.setBaselineAligned(false);power.setContentDescription("Start desktop controls");home=Aero.text(c,"⌂",19,Color.WHITE);home.setContentDescription("Show desktop");home.setGravity(Gravity.CENTER);home.setBackground(Aero.gloss(c,new int[]{0xff5a735c,0xff37543d,0xff293f30,0xff3f5b45},3,0xff9cae9a));home.setOnClickListener(v->actions.open("setting:show-desktop"));TextView lock=Aero.text(c,"▣",17,Color.WHITE);layoutLock=lock;lock.setGravity(Gravity.CENTER);lock.setContentDescription("Lock desktop layout");lock.setBackground(Aero.gloss(c,new int[]{0xff858d91,0xff565e64,0xff303a42,0xff5c676f},2,0xff9aa5ab));lock.setOnClickListener(v->{boolean locked=!store.prefs.getBoolean("layout_locked",false);store.prefs.edit().putBoolean("layout_locked",locked).apply();lock.setSelected(locked);android.widget.Toast.makeText(c,locked?"Desktop layout locked":"Desktop layout unlocked",android.widget.Toast.LENGTH_SHORT).show();});TextView more=Aero.text(c,"›",20,Color.WHITE);more.setGravity(Gravity.CENTER);more.setContentDescription("Start menu options");more.setBackground(Aero.gloss(c,new int[]{0xff858d91,0xff565e64,0xff303a42,0xff5c676f},2,0xff9aa5ab));more.setOnClickListener(v->powerMenu(more));
        for (TextView button : new TextView[]{home, lock, more}) {
            button.setIncludeFontPadding(false);
            LayoutParams cell = new LayoutParams(0, Aero.dp(c, 32), 1);
            if (button != home) cell.leftMargin = Aero.dp(c, 3);
            power.addView(button, cell);
        }
        right.addView(power,new LayoutParams(-1,Aero.dp(c,44)));
        hoverOption(programs,false);hoverOption(home,true);hoverOption(lock,true);hoverOption(more,true);ContextMenus.bind(programs,(x,y)->startOptions(programs));ContextMenus.bind(home,(x,y)->powerMenu(home));ContextMenus.bind(lock,(x,y)->powerMenu(lock));ContextMenus.bind(more,(x,y)->powerMenu(more));refreshHome();
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int n,int f){}public void onTextChanged(CharSequence s,int start,int before,int n){filter();}public void afterTextChanged(Editable e){}});
    }
    private void hoverOption(View view,boolean dark){
        android.graphics.drawable.Drawable normal=view.getBackground();android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
        for(int state:new int[]{android.R.attr.state_pressed,android.R.attr.state_hovered,android.R.attr.state_focused,android.R.attr.state_selected})states.addState(new int[]{state},Aero.gloss(getContext(),dark?new int[]{0xddbed9e8,0xcc76a3bf,0xcc446c88,0xcc80a3bb}:new int[]{0xfff5fbff,0xffe4f1fc,0xffcce5f7,0xffe7f4fd},3,dark?0xffb4cad7:0xff80aed0));
        states.addState(new int[]{},normal==null?Aero.panel(getContext(),0x00ffffff,0x00ffffff,3,0):normal);view.setBackground(states);view.setFocusable(true);

        view.setOnTouchListener((v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_DOWN||e.getActionMasked()==MotionEvent.ACTION_CANCEL)clearHeldOption();return false;});
        view.setOnHoverListener((v,e)->{if(e.getActionMasked()==MotionEvent.ACTION_HOVER_ENTER)clearHeldOption();return false;});view.setOnFocusChangeListener((v,focused)->{if(focused)clearHeldOption();});
    }
    private void clearHeldOption(){if(heldOption!=null){heldOption.setSelected(false);heldOption=null;}}
    void availableHeight(int height){boolean compact=height<Aero.dp(getContext(),420);profile.setVisibility(compact?GONE:VISIBLE);}
    void entries(List<AppCatalog.Entry> values){all.clear();all.addAll(values);filter();if(pendingListPosition>=0&&!values.isEmpty()){list.setSelection(pendingListPosition);pendingListPosition=-1;}}
    void refreshName(){((TextView)profile.getChildAt(1)).setText(store.prefs.getString("desktop_name","Nth Desktop Vista"));View avatar=ProfilePortrait.view(getContext(),store);avatar.setContentDescription("Change profile picture");avatar.setFocusable(true);avatar.setOnClickListener(v->actions.open("setting:page:accounts"));ContextMenus.bind(avatar,(x,y)->navigationMenu(avatar,"setting:page:accounts","User account"));profile.removeViewAt(0);profile.addView(avatar,0,new LayoutParams(Aero.dp(getContext(),49),Aero.dp(getContext(),49)));}
    void refreshHome(){home.setVisibility(VISIBLE);layoutLock.setSelected(store.prefs.getBoolean("layout_locked",false));}
    void show(boolean keyboard){clearHeldOption();refreshHome();refreshName();setVisibility(VISIBLE);bringToFront();allPrograms=false;programFolder="";programs.setText("All Programs  ›");search.setText("");filter();if(Aero.motion(getContext())){setAlpha(0);animate().alpha(1).setDuration(120).start();}else setAlpha(1);if(keyboard){search.requestFocus();search.post(()->((InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(search,InputMethodManager.SHOW_IMPLICIT));}else{search.clearFocus();requestFocus();}}
    void hide(){if(flyout!=null)flyout.dismiss();clearHeldOption();animate().cancel();((InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(getWindowToken(),0);search.clearFocus();setVisibility(GONE);}
    org.json.JSONObject state(){org.json.JSONObject result=new org.json.JSONObject();GadgetLayout.put(result,"visible",getVisibility()==VISIBLE);GadgetLayout.put(result,"all",allPrograms);GadgetLayout.put(result,"programFolder",programFolder);GadgetLayout.put(result,"position",list.getFirstVisiblePosition());GadgetLayout.put(result,"ui",ToolViewState.capture(this));return result;}
    void restore(org.json.JSONObject state){if(state==null||!state.optBoolean("visible"))return;refreshHome();setVisibility(VISIBLE);setAlpha(1);bringToFront();allPrograms=state.optBoolean("all");programFolder=state.optString("programFolder");programs.setText(allPrograms?"‹  Back":"All Programs  ›");ToolViewState.restore(this,state.optJSONObject("ui"));filter();pendingListPosition=state.optInt("position");list.setSelection(pendingListPosition);}
    private void openSearch(String label){actions.open((label.equals("Search Everywhere")?"start-search:":"web-search:")+android.net.Uri.encode(search.getText().toString()));}
    private void activate(StartSearch.Result result){if(result.id.startsWith("start-group:")){programFolder=result.id.substring(12);allPrograms=true;programs.setText("‹  Back");filter();}else actions.open(result.id);}
    private StartSearch.Result builtIn(String id,String name,String kind){return new StartSearch.Result(id,store.name(id,name),"Pinned programs",kind,null,true);}
    private StartSearch.Result result(String id){for(AppCatalog.Entry app:all)if(app.id.equals(id))return new StartSearch.Result(id,store.name(id,app.label),"Programs","programs",app.icon,true);switch(id){case "@browser":return builtIn(id,BrowserPanel.NAME,"browser");case "@notes":return builtIn(id,"Notepad","notes");case "@photos":return builtIn(id,"Photo Gallery","pictures");case MediaLibrary.PLAYER:return builtIn(id,MediaLibrary.PLAYER_TITLE,"musicfile");case MediaLibrary.CENTER:return builtIn(id,MediaLibrary.CENTER_TITLE,"videofile");case "@files":return builtIn(id,"Nth Explorer","files");case "@recycle":return builtIn(id,"Recycle Bin","recycle");case "@workspaces":return builtIn(id,"Saved Workspaces","computer");case "@recent":return builtIn(id,"Recent Items","clock");case "@taskmanager":return builtIn(id,"Task Manager","taskmanager");default:for(String[] tool:new String[][]{{"@gadget:calculator","Calculator","calculator"},{"@gadget:puzzle","Tile Puzzle","puzzle"},{"@gadget:dice","Dice Roller","dice"},{"@sticky","Sticky Notes","sticky"},{"@calendar","Calendar","calendar"},{"@control","Control Panel","control"},{"@welcome","Welcome Center","orb"},{"@computer","Computer","computer"}})if(tool[0].equals(id))return builtIn(id,tool[1],tool[2]);org.json.JSONObject location=FolderLinks.get(store,id);if(location!=null)return builtIn(id,location.optString("title","Folder"),"files");if(store.isFolder(id))return builtIn(id,store.name(id,"Folder"),"folderapps");return null;}}
    private void filter(){
        String query=search.getText().toString().trim();filtered.clear();pinnedCount=0;searchLinks.setVisibility(query.isEmpty()?GONE:VISIBLE);
        if(!query.isEmpty()){for(StartSearch.Result item:StartSearch.find(store,all,query))if(!StartItemMenus.hidden(store,item.id))filtered.add(item);}
        else if(allPrograms){
            if(programFolder.equals("Accessories")){for(String[] item:new String[][]{{"@notes","Notepad","notes"},{"@files","Nth Explorer","files"},{"@photos","Photo Gallery","pictures"},{MediaLibrary.PLAYER,MediaLibrary.PLAYER_TITLE,"musicfile"},{MediaLibrary.CENTER,MediaLibrary.CENTER_TITLE,"videofile"},{"@gadget:calculator","Calculator","calculator"},{"@sticky","Sticky Notes","sticky"},{"@calendar","Calendar","calendar"},{"@taskmanager","Task Manager","taskmanager"},{"@recycle","Recycle Bin","recycle"},{"@workspaces","Saved Workspaces","computer"},{"@recent","Recent Items","clock"}})filtered.add(builtIn(item[0],item[1],item[2]));}
            else if(programFolder.equals("Games")){filtered.add(builtIn("@gadget:puzzle","Tile Puzzle","puzzle"));filtered.add(builtIn("@gadget:dice","Dice Roller","dice"));}
            else{for(String folder:new String[]{"Accessories","Games"})filtered.add(new StartSearch.Result("start-group:"+folder,folder,"Programs","files",null,false));java.util.Iterator<String> ids=store.folders().keys();while(ids.hasNext()){String id=ids.next();filtered.add(builtIn(id,store.name(id,"Folder"),"folderapps"));}for(AppCatalog.Entry app:all)if(!StartItemMenus.hidden(store,app.id))filtered.add(new StartSearch.Result(app.id,store.name(app.id,app.label),"Programs","programs",app.icon,true));}
        }else{
            for(String id:store.startPins()){StartSearch.Result item=result(id);if(item!=null&&!StartItemMenus.hidden(store,id)){filtered.add(item);pinnedCount++;}}
            Set<String> seen=new HashSet<>(store.startPins());for(String id:store.recents()){StartSearch.Result item=result(id);if(item!=null&&!StartItemMenus.hidden(store,id)&&!StartItemMenus.recentHidden(store,id)&&seen.add(id)&&filtered.size()<pinnedCount+8)filtered.add(item);}for(AppCatalog.Entry app:all)if(!StartItemMenus.hidden(store,app.id)&&!StartItemMenus.recentHidden(store,app.id)&&seen.add(app.id)&&filtered.size()<pinnedCount+8)filtered.add(new StartSearch.Result(app.id,store.name(app.id,app.label),"Programs","programs",app.icon,true));
        }
        adapter.notifyDataSetChanged();list.setSelection(0);
    }
    private void itemMenu(View anchor,StartSearch.Result item){
        if(flyout!=null)flyout.dismiss();
        flyout=StartItemMenus.create(getContext(),anchor,store,item,!allPrograms&&search.length()==0,
                actions::open,()->{filter();actions.shortcutsChanged();},this::activate);
        ContextMenus.centered(anchor,(x,y)->ContextMenus.show(flyout,anchor,x,y));
    }
    private void navigationMenu(View anchor,String id,String title){
        if(id.equals("start-recent")){recentMenu(anchor);return;}
        if(flyout!=null)flyout.dismiss();
        flyout=new DesktopContextMenu(getContext(),"Vista "+title+" menu",30,238);
        flyout.item("Open", "empty",()->{if(id.startsWith("start-group:")){allPrograms=true;programFolder=id.substring(12);programs.setText("‹  Back");filter();}else actions.open(id);});
        flyout.defaultLast();
        if(Arrays.asList("@computer","@files","@documents","@pictures","@music").contains(id)){
            flyout.item("Explore","files",()->actions.open(id));
            if(id.equals("@computer"))flyout.item("Manage","control",()->actions.open("@taskmanager"));
            flyout.separator();
            flyout.item("Create Shortcut","empty",()->{if(!store.pinned(id))store.toggle(id);actions.shortcutsChanged();});
        }
        flyout.separator();
        flyout.item("Properties","empty",()->actions.open(id.equals("@computer")?"@system":id.equals("@files")?"setting:page:accounts":id.equals("@control")?"setting:page:taskbar":id));
        ContextMenus.centered(anchor,(x,y)->ContextMenus.show(flyout,anchor,x,y));
    }
    private void startOptions(View anchor){
        if(flyout!=null)flyout.dismiss();
        flyout=new DesktopContextMenu(getContext(),"Vista Start Menu properties",30,238);
        flyout.item("Open","files",()->actions.open("@organize"));
        flyout.item("Explore","files",()->actions.open("@organize"));
        flyout.separator();
        flyout.item("Restore deleted shortcuts","empty",(!store.prefs.getString("start_deleted","[]").equals("[]")||!store.prefs.getString("start_removed_recent","[]").equals("[]")),()->{store.prefs.edit().remove("start_deleted").remove("start_removed_recent").apply();filter();});
        flyout.item("Properties","control",()->actions.open("setting:page:taskbar"));
        ContextMenus.centered(anchor,(x,y)->ContextMenus.show(flyout,anchor,x,y));
    }
    private void recentMenu(View anchor){if(flyout!=null)flyout.dismiss();flyout=new DesktopContextMenu(getContext(),"Recent Items",30,238);org.json.JSONArray items=RecentFiles.list(store);for(int i=0;i<Math.min(10,items.length());i++){org.json.JSONObject item=items.optJSONObject(i);if(item==null)continue;String uri=item.optString("uri"),mime=item.optString("mime"),name=item.optString("name");flyout.item(name,item.optString("kind").equals("folder")?"files":"documentfile",()->{actions.open("recent-file:"+uri);});}if(items.length()==0)flyout.item("No recent documents","empty",false,()->{});else{flyout.separator();flyout.item("Clear Recent Items","empty",()->store.prefs.edit().remove("recent_files").apply());}flyout.separator();flyout.item("View all and history settings…","control",()->actions.open("@recent"));ContextMenus.centered(anchor,(x,y)->ContextMenus.show(flyout,anchor,x,y));}
    private void powerMenu(View anchor){if(flyout!=null)flyout.dismiss();flyout=new DesktopContextMenu(getContext(),"Start menu options",30,214);flyout.item("Show desktop","computer",()->actions.open("setting:show-desktop"));flyout.item("Taskbar and Start Menu…","control",()->actions.open("setting:page:taskbar"));if(!actions.isHome())flyout.item("Make Nth Desktop home","orb",actions::home);flyout.item("Restart Nth Desktop","power",()->actions.open("setting:restart-desktop"));flyout.item("Battery settings","power",()->actions.open("setting:android:battery"));ContextMenus.centered(anchor,(x,y)->ContextMenus.show(flyout,anchor,x,y));}
    private final class ListAdapter extends BaseAdapter{
        public int getCount(){return filtered.size();}public Object getItem(int p){return filtered.get(p);}public long getItemId(int p){return p;}
        public View getView(int pos,View recycled,android.view.ViewGroup parent){
            StartSearch.Result item=filtered.get(pos);LinearLayout row=new LinearLayout(getContext());row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(Aero.dp(getContext(),7),Aero.dp(getContext(),4),Aero.dp(getContext(),5),Aero.dp(getContext(),4));row.setBackground(Aero.selection(getContext()));View icon;if(item.icon!=null){ImageView image=new ImageView(getContext());image.setImageDrawable(item.icon);icon=image;}else icon=new AeroIcon(getContext(),item.kind);row.addView(icon,new LayoutParams(Aero.dp(getContext(),29),Aero.dp(getContext(),29)));TextView name=Aero.text(getContext(),item.title+(search.getText().length()>0?"\n"+item.group:""),12,Aero.INK);name.setMaxLines(2);name.setEllipsize(TextUtils.TruncateAt.END);name.setGravity(Gravity.CENTER_VERTICAL);name.setPadding(Aero.dp(getContext(),8),0,0,0);row.addView(name,new LayoutParams(0,Aero.dp(getContext(),35),1));if(item.id.startsWith("start-group:")||store.isFolder(item.id)){TextView arrow=Aero.text(getContext(),"›",16,Aero.INK);row.addView(arrow,new LayoutParams(Aero.dp(getContext(),12),-1));}if(pinnedCount>0&&pos==pinnedCount-1){android.graphics.drawable.LayerDrawable separator=new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{Aero.selection(getContext()),new android.graphics.drawable.ColorDrawable(0xffd0d7dc)});separator.setLayerGravity(1,Gravity.BOTTOM);separator.setLayerHeight(1,Aero.dp(getContext(),1));separator.setLayerInset(1,Aero.dp(getContext(),7),0,Aero.dp(getContext(),7),0);row.setBackground(separator);row.setPadding(Aero.dp(getContext(),7),Aero.dp(getContext(),4),Aero.dp(getContext(),5),Aero.dp(getContext(),9));}row.setContentDescription("Open "+item.group+": "+item.title);// Context-click listeners make a row clickable. Keep tap and hold on that same
            // row, instead of relying on ListView callbacks which it now intercepts.
            row.setOnClickListener(v->activate(item));
            ContextMenus.bind(row,(x,y)->itemMenu(row,item));
            row.setFocusable(false);return row;
        }
    }
}
