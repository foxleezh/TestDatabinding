## 前言
最近在构建MVVM框架，因为这个框架最重要的就是利用DataBinding框架实现VM与V层的交互（以下所称VM和V都指代这个框架），所以有必要深入研究下DataBinding的原理。本文重点讲解DataBinding的原理，里面会穿插DataBinding的一些基本用法，所以需要读者有一定DataBinding使用经验，不了解DataBinding用法的，请移步[MVVM之DataBinding入门](http://www.jianshu.com/p/dd247d6a562d)、[官方文档](https://developer.android.com/topic/libraries/data-binding/index.html?hl=zh-cn)

## DataBinding做了些什么事
DataBinding主要做了两件事：
1.取代烦琐的findviewbyid，自动在binding中生成对应的view
2.绑定VM层和V层的监听关系，使得VM和V层的交互更简单
这里的交互是指两个方向的交互，一是VM通知V层作改变，比如setText,setColor，二是V层通知VM作改变，比如click事件，edittext的输入

## 取代findviewbyid
DataBinding如何取代那些可恶的findviewbyid的呢？在讲原理前我首先说明一点，DataBinding框架并没有用其他高级的API替代现有的API，只是对原有API的封装，也就是说不管是findviewbyid，还是setText，click事件，DataBinding还是用findviewbyid这些原有的API实现的，只是把它隐藏起来了，我们开发过程中不用自己写，因为框架帮我们写了。下面我就把这些隐藏起来的代码呈现出来，看看它到底用了什么魔法。

#### 1.改造xml
我们在写xml文件的时候，需要在头部加layout，并设置data，你以为这是高级API，其实不然，这个xml在编译的时候会被改造。我们先看一个原始的
``` xml
<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto">

    <data>

        <variable
            name="viewModel"
            type="com.foxlee.testdatabinding.NewsViewModel" />

        <import type="android.view.View" />
    </data>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">

        <TextView
            android:id="@+id/tv_name"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:onText="@{viewModel.name}" />

        <TextView
            android:id="@+id/tv_value"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="50dp"
            app:onText="@{viewModel.value1}" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="50dp" />

        <TextView
            android:id="@+id/tv_value1"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="50dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="50dp"
            app:onText="@{viewModel.value1}" />
    </LinearLayout>
</layout>
</layout>
```
我们再看看最后生成的xml，这个xml在build\intermediates\data-binding-layout-out\debug目录下，你也可以反编译apk来看看
``` xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:tag="layout/fragment_new_list_0">

    <TextView
        android:id="@+id/tv_name"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:tag="binding_1" />

    <TextView
        android:id="@+id/tv_value"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="50dp"
        android:tag="binding_2" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="50dp" />

    <TextView
        android:id="@+id/tv_value1"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="50dp" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="50dp"
        android:tag="binding_3" />
</LinearLayout>
         
```
比较之后就可以看到，我们手动添加的layout，data，以及@{viewModel.name}这些看似高级的API用法，其实在编译后都去掉了，取代他们的是在各个绑定了@{}的View添加一个tag，这些tag以binding_开头，后面接一个数字，这里注意：没有绑定@{}的view不会添加tag，比如上面的tv_value1。然后在根布局里也加了一个tag，名字是"layout/xxxx_xx"。为什么要在xml里面加一些莫明其妙的tag呢，接下来我们看看绑定layout的代码就知道了

#### 2.绑定layout

我们绑定layout的代码有两种，Acitivity里是DataBindingUtil.setContentView，Fragment里是DataBindingUtil.inflate，两个方法调用后都会走到bind这个方法
    private static DataBinderMapper sMapper = new DataBinderMapper();

    static <T extends ViewDataBinding> T bind(DataBindingComponent bindingComponent, View root,
            int layoutId) {
        return (T) sMapper.getDataBinder(bindingComponent, root, layoutId);
    }
```

这个DataBinderMapper是编译器自己生成的一个类，getDataBinder方法如下：
``` java
    public android.databinding.ViewDataBinding getDataBinder(android.databinding.DataBindingComponent bindingComponent, android.view.View view, int layoutId) {
        switch(layoutId) {
                case com.foxlee.testdatabinding.R.layout.fragment_new_list:
                    return com.foxlee.testdatabinding.databinding.FragmentNewListBinding.bind(view, bindingComponent);
        }
        return null;
    }
```
这里通过一个switch判断layoutId，然后调用对应layoutId的xxxBinding类的bind方法，这个xxxBinding也是自动生成的，正好是layout名字转成驼峰标识后加Binding，比如你的layout叫fragment_new_list，这个Binding就叫FragmentNewListBinding，看看该类的bind方法
```java
    public static FragmentNewListBinding bind(android.view.View view, android.databinding.DataBindingComponent bindingComponent) {
        if (!"layout/fragment_new_list_0".equals(view.getTag())) {
            throw new RuntimeException("view tag isn't correct on view:" + view.getTag());
        }
        return new FragmentNewListBinding(bindingComponent, view);
    }
```
首先判断view的tag是不是叫layout/fragment_new_list_0，是不是有点眼熟，这就是之前xml被转变后根布局加上的那个tag！还记得那些binding_1，binding_2吗？接下来也会用到。接着new了一个FragmentNewListBinding，看看构造方法
```java
    public FragmentNewListBinding(android.databinding.DataBindingComponent bindingComponent, View root) {
        super(bindingComponent, root, 1);
        final Object[] bindings = mapBindings(bindingComponent, root, 5, sIncludes, sViewsWithIds);
        this.mboundView0 = (android.widget.LinearLayout) bindings[0];
        this.mboundView0.setTag(null);
        this.mboundView3 = (android.widget.TextView) bindings[3];
        this.mboundView3.setTag(null);
        this.tvName = (android.widget.TextView) bindings[1];
        this.tvName.setTag(null);
        this.tvValue = (android.widget.TextView) bindings[2];
        this.tvValue.setTag(null);
        this.tvValue1 = (android.widget.TextView) bindings[4];
        setRootTag(root);
        // listeners
        invalidateAll();
    }
```
这里似乎有点接近findviewbyid了，通过mapBindings方法得到一个数组，然后将数组里的每个对象强转为FragmentNewListBinding里的各个View,这些View的名字与xml中设置的id有关，如果设置了id就用驼峰标识，没有设置id就用mboundView加上该view在bindings中的下标，这里注意：mapBindings传递了一个参数5，这个值跟bindings数组的数量是一样的，这个数量=绑定了@{}的view+有设置id的view+根布局的view，那些没有设置id，也没有设置@{}的view不在此列。接下来我们看看mapBindings方法，这个方法在父类ViewDataBinding中：
```java
   protected static Object[] mapBindings(DataBindingComponent bindingComponent, View root,
            int numBindings, IncludedLayouts includes, SparseIntArray viewsWithIds) {
        Object[] bindings = new Object[numBindings];
        mapBindings(bindingComponent, root, bindings, includes, viewsWithIds, true);
        return bindings;
    }

   private static void mapBindings(DataBindingComponent bindingComponent, View view,
            Object[] bindings, IncludedLayouts includes, SparseIntArray viewsWithIds,
            boolean isRoot) {
        final int indexInIncludes;
        final ViewDataBinding existingBinding = getBinding(view);
        if (existingBinding != null) {
            return;
        }
        Object objTag = view.getTag();
        final String tag = (objTag instanceof String) ? (String) objTag : null;
        boolean isBound = false;
        if (isRoot && tag != null && tag.startsWith("layout")) {
            final int underscoreIndex = tag.lastIndexOf('_');
            if (underscoreIndex > 0 && isNumeric(tag, underscoreIndex + 1)) {
                final int index = parseTagInt(tag, underscoreIndex + 1);
                if (bindings[index] == null) {
                    bindings[index] = view;
                }
                indexInIncludes = includes == null ? -1 : index;
                isBound = true;
            } else {
                indexInIncludes = -1;
            }
        } else if (tag != null && tag.startsWith(BINDING_TAG_PREFIX)) {
            int tagIndex = parseTagInt(tag, BINDING_NUMBER_START);
            if (bindings[tagIndex] == null) {
                bindings[tagIndex] = view;
            }
            isBound = true;
            indexInIncludes = includes == null ? -1 : tagIndex;
        } else {
            // Not a bound view
            indexInIncludes = -1;
        }
        if (!isBound) {
            final int id = view.getId();
            if (id > 0) {
                int index;
                if (viewsWithIds != null && (index = viewsWithIds.get(id, -1)) >= 0 &&
                        bindings[index] == null) {
                    bindings[index] = view;
                }
            }
        }

        if (view instanceof  ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) view;
            final int count = viewGroup.getChildCount();
            int minInclude = 0;
            for (int i = 0; i < count; i++) {
                final View child = viewGroup.getChildAt(i);
                boolean isInclude = false;
                if (indexInIncludes >= 0 && child.getTag() instanceof String) {
                    String childTag = (String) child.getTag();
                    if (childTag.endsWith("_0") &&
                            childTag.startsWith("layout") && childTag.indexOf('/') > 0) {
                        // This *could* be an include. Test against the expected includes.
                        int includeIndex = findIncludeIndex(childTag, minInclude,
                                includes, indexInIncludes);
                        if (includeIndex >= 0) {
                            isInclude = true;
                            minInclude = includeIndex + 1;
                            final int index = includes.indexes[indexInIncludes][includeIndex];
                            final int layoutId = includes.layoutIds[indexInIncludes][includeIndex];
                            int lastMatchingIndex = findLastMatching(viewGroup, i);
                            if (lastMatchingIndex == i) {
                                bindings[index] = DataBindingUtil.bind(bindingComponent, child,
                                        layoutId);
                            } else {
                                final int includeCount =  lastMatchingIndex - i + 1;
                                final View[] included = new View[includeCount];
                                for (int j = 0; j < includeCount; j++) {
                                    included[j] = viewGroup.getChildAt(i + j);
                                }
                                bindings[index] = DataBindingUtil.bind(bindingComponent, included,
                                        layoutId);
                                i += includeCount - 1;
                            }
                        }
                    }
                }
                if (!isInclude) {
                    mapBindings(bindingComponent, child, bindings, includes, viewsWithIds, false);
                }
            }
        }
    }
```
第一个方法初始化了bindinds数组，大小等于传进来的numBindings，然后调用另一个mapBindings方法，这个方法比较长，我们一段段分析
```java
======华丽分割线========================================
        //定义常量indexInIncludes
        final int indexInIncludes;
        //是否初始化过binding，如果初始化过直接return
        final ViewDataBinding existingBinding = getBinding(view);
        if (existingBinding != null) {
            return;
        }
        Object objTag = view.getTag();
        //获得该view的tag，也就是layout/fragment_new_list_0或者binding_1,binding_2等
        final String tag = (objTag instanceof String) ? (String) objTag : null;
        boolean isBound = false;
======华丽分割线========================================

    static ViewDataBinding getBinding(View v) {
        if (v != null) {
            if (USE_TAG_ID) {
                return (ViewDataBinding) v.getTag(R.id.dataBinding);
            } else {
                final Object tag = v.getTag();
                if (tag instanceof ViewDataBinding) {
                    return (ViewDataBinding) tag;
                }
            }
        }
        return null;
    }

    //这个方法在FragmentNewListBinding的构造方法中有调用，api14以下不同处理
    protected void setRootTag(View view) {
        if (USE_TAG_ID) {
            view.setTag(R.id.dataBinding, this);
        } else {
            view.setTag(this);
        }
    }

    //兼容api14以下
    private static final boolean USE_TAG_ID = DataBinderMapper.TARGET_MIN_SDK >= 14;

```
这段主要是防止多次初始化，然后获得view的tag
```java
======华丽分割线========================================
       //isRoot判断是否为根布局，这是方法传进来的值，接着判断是否以layout开头，这里满足条件的是layout/fragment_new_list_0
       if (isRoot && tag != null && tag.startsWith("layout")) {
            final int underscoreIndex = tag.lastIndexOf('_');
            //underscoreIndex是下划线的下标，isNumeric方法是判断下划线后面的是不是数字
            if (underscoreIndex > 0 && isNumeric(tag, underscoreIndex + 1)) {
                final int index = parseTagInt(tag, underscoreIndex + 1);
                //得到下划线后面的数字，然后把view装进bindings对应的位置，这里bindings[0]就是根布局LinearLayout
                if (bindings[index] == null) {
                    bindings[index] = view;
                }
                indexInIncludes = includes == null ? -1 : index;
                //是否已绑定
                isBound = true;
            } else {
                indexInIncludes = -1;
            }
        }
        //BINDING_TAG_PREFIX="binding_"，这里满足条件的是binding_1,binding_2等
         else if (tag != null && tag.startsWith(BINDING_TAG_PREFIX)) {
            //BINDING_NUMBER_START=BINDING_TAG_PREFIX.length();
            int tagIndex = parseTagInt(tag, BINDING_NUMBER_START);
            //得到下划线后面的数字，然后装进bindings对应位置，这里bingding[1],bindings[2]就是对应的TextView
            if (bindings[tagIndex] == null) {
                bindings[tagIndex] = view;
            }
            isBound = true;
            indexInIncludes = includes == null ? -1 : tagIndex;
        } else {
            // Not a bound view
            indexInIncludes = -1;
        }
        //如果传进来的view的tag没有以layout开头，也没有以binding_开头,对应的是tv_value1这个view
        if (!isBound) {
            final int id = view.getId();
            if (id > 0) {
                int index;
                //viewsWithIds是传进来的值，该值在FragmentNewListBinding的静态代码块中初始化，下面有代码,这样bindings[4]就是tv_value1这个id对应的TextView了
                if (viewsWithIds != null && (index = viewsWithIds.get(id, -1)) >= 0 &&
                        bindings[index] == null) {
                    bindings[index] = view;
                }
            }
        }
======华丽分割线========================================


    private static final android.util.SparseIntArray sViewsWithIds;
    static {
        sIncludes = null;
        sViewsWithIds = new android.util.SparseIntArray();
        sViewsWithIds.put(R.id.tv_value1, 4);
    }
```
这段代码做了主要的赋值操作，将xml中的view一个个装进bindings这个数组，分三种情况，一是根布局，二是设置了@{}的view,三是设置了id的view，优先级就是根布局>@{}>id
```java
        //如果view是ViewGroup
        if (view instanceof  ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) view;
            final int count = viewGroup.getChildCount();
            int minInclude = 0;
            for (int i = 0; i < count; i++) {
                final View child = viewGroup.getChildAt(i);
                boolean isInclude = false;
                //indexInIncludes = includes == null ? -1 : index;这里includes为空，暂时不分析这段
                if (indexInIncludes >= 0 && child.getTag() instanceof String) {
                    String childTag = (String) child.getTag();
                    if (childTag.endsWith("_0") &&
                            childTag.startsWith("layout") && childTag.indexOf('/') > 0) {
                        // This *could* be an include. Test against the expected includes.
                        int includeIndex = findIncludeIndex(childTag, minInclude,
                                includes, indexInIncludes);
                        if (includeIndex >= 0) {
                            isInclude = true;
                            minInclude = includeIndex + 1;
                            final int index = includes.indexes[indexInIncludes][includeIndex];
                            final int layoutId = includes.layoutIds[indexInIncludes][includeIndex];
                            int lastMatchingIndex = findLastMatching(viewGroup, i);
                            if (lastMatchingIndex == i) {
                                bindings[index] = DataBindingUtil.bind(bindingComponent, child,
                                        layoutId);
                            } else {
                                final int includeCount =  lastMatchingIndex - i + 1;
                                final View[] included = new View[includeCount];
                                for (int j = 0; j < includeCount; j++) {
                                    included[j] = viewGroup.getChildAt(i + j);
                                }
                                bindings[index] = DataBindingUtil.bind(bindingComponent, included,
                                        layoutId);
                                i += includeCount - 1;
                            }
                        }
                    }
                }
                //如果不是include的，就递归该方法，isRoot值传false，就会走上一步中的第二种或第三种
                if (!isInclude) {
                    mapBindings(bindingComponent, child, bindings, includes, viewsWithIds, false);
                }
            }
        }
```
这段方法主要是有个递归方法，重复执行mapBindings方法，这样一层层的递归下去就会把所有的view都装进bindings数组了

至此，xml中所有的View就一一绑定到FragmentNewListBinding的成员变量上去了，我们在代码中就可以用FragmentNewListBinding获得xml中各个view

## 绑定VM和V的交互
#### 1.设置VM
在开发中完成VM和V的绑定需要binding.setVariable或者binding.setViewModel，两者效果一样，因为setVariable会间接调用setViewModel方法
```java
    public boolean setVariable(int variableId, Object variable) {
        switch(variableId) {
            case BR.viewModel :
                setViewModel((com.foxlee.testdatabinding.NewsViewModel) variable);
                return true;
        }
        return false;
    }
```
值得注意的是，setVariable一定会有，setViewModel是根据xml中data定义的name生成的，也就是说name="viewModel"，会生成一个叫setViewModel的方法，如果把name改为AAA，那么就会生成一个叫setAAA的方法，你设置多少个variable，就会生成多少个setXXX方法
```xml
    <data>
        <variable
            name="AAA"
            type="com.foxlee.testdatabinding.NewsViewModel" />
        <variable
            name="stringA"
            type="String" />
    </data>
```
```java
    public boolean setVariable(int variableId, Object variable) {
        switch(variableId) {
            case BR.stringA :
                setStringA((java.lang.String) variable);
                return true;
            case BR.AAA :
                setAAA((com.foxlee.testdatabinding.NewsViewModel) variable);
                return true;
        }
        return false;
    }
```
所以我们在用的时候就要注意了，不要乱写，要根据xml定义的name来写，而setVariable一定会有，但是要传递一个id值，这个id值是BR中的，BR文件是一个Map表，作用跟R文件差不多，会自动生成
```java
package com.foxlee.testdatabinding;

public class BR {
        public static final int _all = 0;
        public static final int name = 1;
        public static final int value1 = 2;
        public static final int viewModel = 3;
}
```
哪些情况会生成BR文件中的值呢，有三种
1.xml中中设置variable的name属性
```xml
 <data>
        <variable
            name="viewModel"
            type="com.foxlee.testdatabinding.NewsViewModel" />
        <import type="android.view.View" />
    </data>
```
2.VM继承BaseObservable，将某个成员变量加上@Bindable注解
```java
    @Bindable
    public String name;
```
3.VM继承BaseObservable，将get,set,is开头的方法加上@Bindable注解
```java
    @Bindable
    public String getValue2(){
        return "test";
    }
    @Bindable
    public void setValue3(String s){

    }
    @Bindable
    public boolean isvalue4(){
        return true;
    }
```
注意这里的get和is方法不能带参数，必须有返回值，set方法不能带返回值，必须且只能带一个参数，不然编译报错。方法后面的Value2,Value3等第一个字母会自动转为小写，BR文件中就是value2,value3，不会是Value2,Value3。


我们接着上面的setVariable方法讲，这个方法传的id值必须与xml中variable的name一致，不然就不会调用setViewModel方法，我们看setViewModel方法干了什么
```java
    public void setViewModel(com.foxlee.testdatabinding.NewsViewModel ViewModel) {
        updateRegistration(0, ViewModel);
        this.mViewModel = ViewModel;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.viewModel);
        super.requestRebind();
    }
```
这个方法主要是调用updateRegistration方法，然后将mDirtyFlags |= 0x1L，再调用notifyPropertyChanged(BR.viewModel)，主要做的工作是updateRegistration注册监听器，notifyPropertyChanged调用监听器回调，我们先看updateRegistration,这个方法在父类
```java
 protected boolean updateRegistration(int localFieldId, Observable observable) {
        return updateRegistration(localFieldId, observable, CREATE_PROPERTY_LISTENER);
    }

    /**
     * @hide
     */
    protected boolean updateRegistration(int localFieldId, ObservableList observable) {
        return updateRegistration(localFieldId, observable, CREATE_LIST_LISTENER);
    }

    /**
     * @hide
     */
    protected boolean updateRegistration(int localFieldId, ObservableMap observable) {
        return updateRegistration(localFieldId, observable, CREATE_MAP_LISTENER);
    }
```
这里有三个重载的方法，分别对应Observable ，ObservableList 和ObservableMap ，我们一般用到的是Observable ，我们VM是继承自BaseObservable的，BaseObservable实现Observable接口，那我们先只看第一个方法，这个方法传递了一个CREATE_PROPERTY_LISTENER参数
```java
    private static final CreateWeakListener CREATE_PROPERTY_LISTENER = new CreateWeakListener() {
        @Override
        public WeakListener create(ViewDataBinding viewDataBinding, int localFieldId) {
            return new WeakPropertyListener(viewDataBinding, localFieldId).getListener();
        }
    };
```
这是一个接口回调，当调用CREATE_PROPERTY_LISTENER的create方法时就会返回一个WeakPropertyListener的实例，我们先接着上面的updateRegistration方法看
```java
    private boolean updateRegistration(int localFieldId, Object observable,
            CreateWeakListener listenerCreator) {
        if (observable == null) {
            return unregisterFrom(localFieldId);
        }
        WeakListener listener = mLocalFieldObservers[localFieldId];
        if (listener == null) {
            registerTo(localFieldId, observable, listenerCreator);
            return true;
        }
        if (listener.getTarget() == observable) {
            return false;//nothing to do, same object
        }
        unregisterFrom(localFieldId);
        registerTo(localFieldId, observable, listenerCreator);
        return true;
    }
```
这里主要有两个方法的调用，一个registerTo(localFieldId, observable, listenerCreator)，一个unregisterFrom(localFieldId)，其他的代码是处理不同情况对两个方法的调用，registerTo就是注册监听器，unregisterFrom就是删除监听器，以上其他代码的逻辑主要处理三种情况
1.observable传进来为null，删除监听器
2.mLocalFieldObservers[localFieldId]为空，也就是第一次注册，那么就注册监听器
3.mLocalFieldObservers[localFieldId]不为空并且里面的监听器和传进来监听器不一致，先删除监听器，再重新注册新的监听器

我们先看看注册监听器的方法
```java
    protected void registerTo(int localFieldId, Object observable,
            CreateWeakListener listenerCreator) {
        if (observable == null) {
            return;
        }
        WeakListener listener = mLocalFieldObservers[localFieldId];
        if (listener == null) {
            listener = listenerCreator.create(this, localFieldId);
            mLocalFieldObservers[localFieldId] = listener;
        }
        listener.setTarget(observable);
    }
```
listenerCreator就是之前的那个CREATE_PROPERTY_LISTENER，调用create方法就是调用
return new WeakPropertyListener(viewDataBinding, localFieldId).getListener()，我们看一下这个类
```java
private static class WeakPropertyListener extends Observable.OnPropertyChangedCallback
            implements ObservableReference<Observable> {
        final WeakListener<Observable> mListener;

        public WeakPropertyListener(ViewDataBinding binder, int localFieldId) {
            mListener = new WeakListener<Observable>(binder, localFieldId, this);
        }

        @Override
        public WeakListener<Observable> getListener() {
            return mListener;
        }

        @Override
        public void addListener(Observable target) {
            target.addOnPropertyChangedCallback(this);
        }

        @Override
        public void removeListener(Observable target) {
            target.removeOnPropertyChangedCallback(this);
        }

        @Override
        public void onPropertyChanged(Observable sender, int propertyId) {
            ViewDataBinding binder = mListener.getBinder();
            if (binder == null) {
                return;
            }
            Observable obj = mListener.getTarget();
            if (obj != sender) {
                return; // notification from the wrong object?
            }
            binder.handleFieldChange(mListener.mLocalFieldId, sender, propertyId);
        }
    }
```
它里面有一个包装类WeakListener，主要用于避免ViewDataBinding内存泄漏，同时储存VM对象
```java
    private static class WeakListener<T> extends WeakReference<ViewDataBinding> {
        private final ObservableReference<T> mObservable;
        protected final int mLocalFieldId;
        private T mTarget;

        public WeakListener(ViewDataBinding binder, int localFieldId,
                ObservableReference<T> observable) {
            super(binder, sReferenceQueue);
            mLocalFieldId = localFieldId;
            mObservable = observable;
        }

        public void setTarget(T object) {
            unregister();
            mTarget = object;
            if (mTarget != null) {
                mObservable.addListener(mTarget);
            }
        }

        public boolean unregister() {
            boolean unregistered = false;
            if (mTarget != null) {
                mObservable.removeListener(mTarget);
                unregistered = true;
            }
            mTarget = null;
            return unregistered;
        }

        public T getTarget() {
            return mTarget;
        }

        protected ViewDataBinding getBinder() {
            ViewDataBinding binder = get();
            if (binder == null) {
                unregister(); // The binder is dead
            }
            return binder;
        }
    }

```
这样mLocalFieldObservers就储存了一个WeakListener对象，这个WeakListener既持有ViewDataBinding的引用，也持有VM的引用，还持有WeakPropertyListener的引用。
registerTo最后调用了listener.setTarget(observable)方法，
这个方法就是调用WeakPropertyListener 的addListener方法，
也就是调用VM的addOnPropertyChangedCallback(this)，
我们看看VM的父类BaseObservable的addOnPropertyChangedCallback方法
```java
    @Override
    public void addOnPropertyChangedCallback(OnPropertyChangedCallback callback) {
        synchronized (this) {
            if (mCallbacks == null) {
                mCallbacks = new PropertyChangeRegistry();
            }
        }
        mCallbacks.add(callback);
    }
```
这个方法就是new了一个PropertyChangeRegistry的实例然后把WeakPropertyListener这个监听器设置给PropertyChangeRegistry，这样VM就持有了PropertyChangeRegistry的引用，也就建立了和ViewDataBinding的联系

删除监听器的方法比较简单,就是调用WeakPropertyListener的unregister方法，然后把刚才建立的联系取消掉
```java
   protected boolean unregisterFrom(int localFieldId) {
        WeakListener listener = mLocalFieldObservers[localFieldId];
        if (listener != null) {
            return listener.unregister();
        }
        return false;
    }

    public boolean unregister() {
            boolean unregistered = false;
            if (mTarget != null) {
                mObservable.removeListener(mTarget);
                unregistered = true;
            }
            mTarget = null;
            return unregistered;
        }
```

至此，VM到ViewDataBinding的通信就通过WeakPropertyListener建立起来了，其实ViewDataBinding一开始就有VM的引用，刚才这么多的逻辑只是为了建立VM到ViewDataBinding的通信，而ViewDataBinding是持有V层各个View的引用的，这样VM和V之间的交互就通过ViewDataBinding这个桥梁建立起来了。这里应该用一个图来说明这些关系

![drawing](http://upload-images.jianshu.io/upload_images/3387045-7534a60360ad0ede.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

#### 2.VM回调通知V
这些类之间的联系搞清楚之后，我们再看VM是怎么通知View进行改变的。之前我们在xml里面写了一些@{}的代码
```xml
        <TextView
            android:id="@+id/tv_name"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:onText="@{viewModel.name}" />
```
然后在VM中写了一个回调方法
```java
    @BindingAdapter("onText")
    public static void onTestChange(TextView view,String text){
        view.setText(text);
        Log.d("onText", "onTestChange: "+text);
    }
```
这些代码会在ViewDataBinding中生成一些处理代码
```java
    @Override
    protected void executeBindings() {
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
        java.lang.String viewModelName = null;
        java.lang.String viewModelValue1 = null;
        com.foxlee.testdatabinding.NewsViewModel viewModel = mViewModel;

        if ((dirtyFlags & 0xfL) != 0) {


            if ((dirtyFlags & 0xbL) != 0) {

                    if (viewModel != null) {
                        // read viewModel.name
                        viewModelName = viewModel.name;
                    }
            }
            if ((dirtyFlags & 0xdL) != 0) {

                    if (viewModel != null) {
                        // read viewModel.value1
                        viewModelValue1 = viewModel.value1;
                    }
            }
        }
        // batch finished
        if ((dirtyFlags & 0xdL) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.mboundView3, viewModelValue1);
            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.tvValue, viewModelValue1);
        }
        if ((dirtyFlags & 0xbL) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.tvName, viewModelName);
        }
    }
```
也就是说当执行executeBindings方法，并且dirtyFlags 满足一定条件的时候，就会执行我们定义好的回调方法，这也是为什么我们在定义回调方法时必须用static方法的原因，因为这个地方是直接用类名调用的。
我们先分析executeBindings在什么地方回调的，然后再分析dirtyFlags值的逻辑。

我们在executeBindings的方法上打个断点

![截图](http://upload-images.jianshu.io/upload_images/3387045-aa859655faf1b414.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
能找到的最前面的代码是
```java
            mFrameCallback = new Choreographer.FrameCallback() {
                @Override
                public void doFrame(long frameTimeNanos) {
                    mRebindRunnable.run();
                }
            };
```
mRebindRunnable.run()这一步是一个回调，调用的地方只有一个
```java
    protected void requestRebind() {
        if (mContainingBinding != null) {
            mContainingBinding.requestRebind();
        } else {
            synchronized (this) {
                if (mPendingRebind) {
                    return;
                }
                mPendingRebind = true;
            }
            if (USE_CHOREOGRAPHER) {
                mChoreographer.postFrameCallback(mFrameCallback);
            } else {
                mUIThreadHandler.post(mRebindRunnable);
            }
        }
    }
```
mChoreographer.postFrameCallback(mFrameCallback);这一步就会触发回调，我们在requestRebind方法处再打一个断点

![截图](http://upload-images.jianshu.io/upload_images/3387045-e7e16d3588f0edc9.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)

这下调用关系就清楚明了了，其实也就是我们之前那个注册关系，
VM调用PropertyChangeRegistry的notifyCallbacks，
然后调用到WeakPropertyListener的onPropertyChanged，
然后调用ViewDataBinding的handleFieldChange，
然后调用requestRebind

executeBindings方法的调用关系清楚后，我们再看看dirtyFlags的逻辑
```java
        long dirtyFlags = 0;
        synchronized(this) {
            dirtyFlags = mDirtyFlags;
            mDirtyFlags = 0;
        }
```
这个值是由mDirtyFlags决定的，而且每次赋值之后都会把mDirtyFlags置为0，mDirtyFlags改变的地方有三个
1.invalidateAll
```java
    @Override
    public void invalidateAll() {
        synchronized(this) {
                mDirtyFlags = 0x8L;
        }
        requestRebind();
    }
```
这个方法会在ViewDataBinding的构造方法中调用
2.setViewModel
```java
     public void setViewModel(com.foxlee.testdatabinding.NewsViewModel ViewModel) {
        updateRegistration(0, ViewModel);
        this.mViewModel = ViewModel;
        synchronized(this) {
            mDirtyFlags |= 0x1L;
        }
        notifyPropertyChanged(BR.viewModel);
        super.requestRebind();
    }
```
这个方法在setVariable中调用
2.onChangeViewModel
```java
    private boolean onChangeViewModel(com.foxlee.testdatabinding.NewsViewModel ViewModel, int fieldId) {
        switch (fieldId) {
            case BR.name: {
                synchronized(this) {
                        mDirtyFlags |= 0x2L;
                }
                return true;
            }
            case BR.value1: {
                synchronized(this) {
                        mDirtyFlags |= 0x4L;
                }
                return true;
            }
            case BR._all: {
                synchronized(this) {
                        mDirtyFlags |= 0x1L;
                }
                return true;
            }
        }
        return false;
    }
```
这个方法在handleFieldChange方法中调用，从之前的分析可知，在VM中调用notifyPropertyChanged()就会执行在handleFieldChange方法，而且notifyPropertyChanged传递进来的BR值正好就是这个switch语句判断的条件，当调用notifyChange时BR为0，也就是BR._all


分析完了之后我们就可以得出结论，在初始化的时候因为会先后调用第一和第二种情况，计算后的结果是9，调用其他notifyPropertyChanged的时候，就是对应的1，2，4

我们再看看executeBindings中对mDirtyFlags的判断
```java
        if ((dirtyFlags & 0xdL) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.mboundView3, viewModelValue1);
            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.tvValue, viewModelValue1);
        }
        if ((dirtyFlags & 0xbL) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.tvName, viewModelName);
        }
```
这些值有什么联系呢，看似0xd,0xb这些没什么特殊的，我们多写几个BR值来看看
```java
 // batch finished
        if ((dirtyFlags & 0x49L) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.mboundView3, viewModelValue2);
        }
        if ((dirtyFlags & 0x51L) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.mboundView4, viewModelValue3);
        }
        if ((dirtyFlags & 0x61L) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.mboundView5, viewModelValue4);
        }
        if ((dirtyFlags & 0x43L) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.tvName, viewModelName);
        }
        if ((dirtyFlags & 0x45L) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.tvValue, viewModelValue1);
        }
```
变成了0x43,0x45,0x49,0x51,0x61,这些都是些什么数字啊，感觉毫无规律可循，不过经验告诉我，这种&|运算一般都跟二进制有关，我们把这些十六进制的转换为二进制看看

![截图](http://upload-images.jianshu.io/upload_images/3387045-97f8afac0b25fd8e.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)
看到规律了吧，这些数字都以1开头，以1结尾，中间递进的为1，这样就可以明白那些判断条件的逻辑了，当
dirtyFlags为1或者为最高位的1000000，或者为10000001时，所有的判断条件都满足，所有的回调都会执行，当dirtyFlags为其中某一个二进制的整数2，4，8，16等等的时候，就只有对应条件能执行了。但是这样我们会有一个疑问，dirtyFlags为long，有长度限制，万一超过64种BR怎么办呢，我们来看看超过64个的情况
```java
        if ((dirtyFlags & 0x2000000000000001L) != 0 || (dirtyFlags_1 & 0x4L) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.mboundView61, viewModelValue60);
        }
        if ((dirtyFlags & 0x4000000000000001L) != 0 || (dirtyFlags_1 & 0x4L) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.mboundView62, viewModelValue61);
        }
        if ((dirtyFlags & 0x8000000000000001L) != 0 || (dirtyFlags_1 & 0x4L) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.mboundView63, viewModelValue62);
        }
        if ((dirtyFlags & 0x1L) != 0 || (dirtyFlags_1 & 0x5L) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.mboundView64, viewModelValue63);
        }
        if ((dirtyFlags & 0x1L) != 0 || (dirtyFlags_1 & 0x6L) != 0) {
            // api target 1

            com.foxlee.testdatabinding.NewsViewModel.onTestChange(this.mboundView65, viewModelValue64);
        }
```
哈哈，它加了另外一个dirtyFlags_1 来一起判断，相当于延长了那个64位1000...1，真是佩服google的程序员！后来想想，干嘛这个地方要用这么复杂的一个设计，直接用switch判断BR的值不行吗？但是这样会漏掉一个情况，如果想让所有的回调都执行呢，那我不是要把所有回调写两遍？因为得有一个case把所有回调再写一遍，所以还是觉得google这种算法精妙。

至此，我们把DataBinding框架大体的逻辑搞清楚了，小结一下吧

## 小结
###### 1.DataBindingUtil.setContentView方法将xml中的各个View赋值给ViewDataBinding，完成findviewbyid的任务
###### 2.ViewDataBinding的setVariable方法建立了ViewDataBinding与VM之间的联系，也就搭建了一个可以互相通信的桥梁
###### 3.当VM层调用notifyPropertyChanged方法时，最终在ViewDataBinding的executeBindings方法中处理逻辑
