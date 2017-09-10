package com.foxlee.testdatabinding;

import android.content.Context;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.databinding.BindingAdapter;
import android.util.Log;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by kelin on 16-4-25.
 */
public class NewsViewModel extends BaseObservable {

    int count=0;

    Timer t=new Timer();
    TimerTask task=new TimerTask() {
        @Override
        public void run() {
            count++;
            name="name"+count;
            value1="value1 "+count;
//            value2="value2 "+count;
//            value3="value3 "+count;
//            value4="value4 "+count;
            notifyPropertyChanged(BR.value1);
        }
    };


    @Bindable
    public String name;

    @Bindable
    public String value1;

    public boolean value2;

    @Bindable
    public String getValue2(){
        return "test";
    }

    @Bindable
    public void setValue3(String value){

    }

    @Bindable
    public boolean isvalue4(){
        return true;
    }
//    @Bindable
//    public String value2;
//    @Bindable
//    public String value3;
//    @Bindable
//    public String value4;
//    @Bindable
//    public String value5;
//    @Bindable
//    public String value6;
//    @Bindable
//    public String value7;
//    @Bindable
//    public String value8;
//    @Bindable
//    public String value9;
//    @Bindable
//    public String value10;
//    @Bindable
//    public String value11;
//    @Bindable
//    public String value12;
//    @Bindable
//    public String value13;
//    @Bindable
//    public String value14;
//    @Bindable
//    public String value15;
//    @Bindable
//    public String value16;
//    @Bindable
//    public String value17;
//    @Bindable
//    public String value18;
//    @Bindable
//    public String value19;
//    @Bindable
//    public String value20;
//    @Bindable
//    public String value21;
//    @Bindable
//    public String value22;
//    @Bindable
//    public String value23;
//    @Bindable
//    public String value24;
//    @Bindable
//    public String value25;
//    @Bindable
//    public String value26;
//    @Bindable
//    public String value27;
//    @Bindable
//    public String value28;
//    @Bindable
//    public String value29;
//    @Bindable
//    public String value30;
//    @Bindable
//    public String value31;
//    @Bindable
//    public String value32;
//    @Bindable
//    public String value33;
//    @Bindable
//    public String value34;
//    @Bindable
//    public String value35;
//    @Bindable
//    public String value36;
//    @Bindable
//    public String value37;
//    @Bindable
//    public String value38;
//    @Bindable
//    public String value39;
//    @Bindable
//    public String value40;
//    @Bindable
//    public String value41;
//    @Bindable
//    public String value42;
//    @Bindable
//    public String value43;
//    @Bindable
//    public String value44;
//    @Bindable
//    public String value45;
//    @Bindable
//    public String value46;
//    @Bindable
//    public String value47;
//    @Bindable
//    public String value48;
//    @Bindable
//    public String value49;
//    @Bindable
//    public String value50;
//    @Bindable
//    public String value51;
//    @Bindable
//    public String value52;
//    @Bindable
//    public String value53;
//    @Bindable
//    public String value54;
//    @Bindable
//    public String value55;
//    @Bindable
//    public String value56;
//    @Bindable
//    public String value57;
//    @Bindable
//    public String value58;
//    @Bindable
//    public String value59;
//    @Bindable
//    public String value60;
//    @Bindable
//    public String value61;
//    @Bindable
//    public String value62;
//    @Bindable
//    public String value63;
//    @Bindable
//    public String value64;

    @BindingAdapter("onText")
    public static void onTestChange(TextView view,String text){
        view.setText(text);
        Log.d("onText", "onTestChange: "+text);
    }

    public NewsViewModel(Context context) {
        t.schedule(task,1000,5000);
    }
}
