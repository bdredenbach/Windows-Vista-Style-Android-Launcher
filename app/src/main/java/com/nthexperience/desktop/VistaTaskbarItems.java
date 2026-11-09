package com.nthexperience.desktop;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

/** Vista keeps Quick Launch separate; each section fills columns from top to bottom. */
final class VistaTaskbarItems extends LinearLayout {
    private boolean expanded;
    VistaTaskbarItems(Context context){super(context);}
    void expanded(boolean value){if(expanded!=value){expanded=value;requestLayout();}}
    @Override protected void onMeasure(int width,int height){
        if(!expanded){super.onMeasure(width,height);return;}
        int total=0,column=0,row=0,cell=Aero.dp(getContext(),48);
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);LayoutParams lp=(LayoutParams)child.getLayoutParams();
            boolean divider=child instanceof TaskbarSeparator;
            if(divider&&row!=0){total+=column;column=0;row=0;}
            child.measure(MeasureSpec.makeMeasureSpec(Math.max(0,lp.width),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(divider?cell*2:cell,MeasureSpec.EXACTLY));
            column=Math.max(column,child.getMeasuredWidth()+lp.rightMargin);
            if(divider||++row==2){total+=column;column=0;row=0;}
        }
        total+=column;
        setMeasuredDimension(Math.max(total,MeasureSpec.getMode(width)==MeasureSpec.EXACTLY?MeasureSpec.getSize(width):getSuggestedMinimumWidth()),resolveSize(cell*2,height));
    }
    @Override protected void onLayout(boolean changed,int left,int top,int right,int bottom){
        if(!expanded){super.onLayout(changed,left,top,right,bottom);return;}
        int x=0,column=0,row=0,cell=Aero.dp(getContext(),48);
        for(int i=0;i<getChildCount();i++){
            View child=getChildAt(i);LayoutParams lp=(LayoutParams)child.getLayoutParams();
            boolean divider=child instanceof TaskbarSeparator;
            if(divider&&row!=0){x+=column;column=0;row=0;}
            child.layout(x,row*cell,x+child.getMeasuredWidth(),row*cell+child.getMeasuredHeight());
            column=Math.max(column,child.getMeasuredWidth()+lp.rightMargin);
            if(divider||++row==2){x+=column;column=0;row=0;}
        }
    }
}
