package com.kangjj.custom.butterknife.complier;

import com.google.auto.service.AutoService;
import com.kangjj.custom.butterknife.annotation.BindView;
import com.kangjj.custom.butterknife.annotation.OnClick;

import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * @Description: // 通过auto-service中的@AutoService可以自动生成AutoService 注解处理器是Google开发的，
 * @Author: jj.kang
 * @Email: 345498912@qq.com
 * @ProjectName: 3.6.3_Custom_ButterKnife
 * @Package: com.kangjj.custom.butterknife.complier
 * @CreateDate: 2019/12/13 22:41
 */
//@AutoService(Processor.class)
public class ButterKnifeProcessor extends AbstractProcessor {
    private Messager messager;
    private Elements elementUtils;
    private Filer filer;            //用来创建新的源文件，class文件以及辅助文件
    private Types typeUtils;        //Types中包含用于操作TypeMirror的工具方法
    private String activityName;        //包名+类名

    // 该方法主要用于一些初始化的操作，通过该方法的参数ProcessingEnvironment可以获取一些列有用的工具类
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        typeUtils = processingEnv.getTypeUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        //添加支持BindView注解的类型
        types.add(BindView.class.getCanonicalName());
        types.add(OnClick.class.getCanonicalName());
        return types;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        //返回次注释Processor支持的最新的源版本，该方法可以通过注解@SupportedSourceVersion指定，详情请看javapoet版本
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {
        if(set!=null && set.size()>0){
            messager.printMessage(Diagnostic.Kind.NOTE,"start-------------");
            // 保存键值对，key是com.kangjj.custom.butterknife.MainActivity   value是所有带BindView注解的属性集合
            Map<String, List<VariableElement>> bindViewMap = new HashMap<>();
            Set<? extends Element> bindViewSet = roundEnv.getElementsAnnotatedWith(BindView.class);
            for (Element element : bindViewSet) {
                VariableElement variableElement = (VariableElement) element;
                // 通过属性元素获取它所属的MainActivity类名，如：com.kangjj.custom.butterknife.MainActivity
                activityName = getActivityName(variableElement);
                List<VariableElement> list = bindViewMap.get(activityName);
                if(list == null){
                    list = new ArrayList<>();
                    // 先加入map集合，引用变量list可以动态改变值
                    bindViewMap.put(activityName,list);
                }
                //将MainActivity所有带BindView属性加入到list集合
                list.add(variableElement);
                messager.printMessage(Diagnostic.Kind.NOTE,"variableElement >>> "+variableElement.getSimpleName().toString());
            }//end for
            // 保存键值对，key是com.kangjj.custom.butterknife.MainActivity   value是所有带OnClick注解的方法集合
            Map<String, List<ExecutableElement>> onClickMap = new HashMap<>();
            Set<? extends Element> onClickSet = roundEnv.getElementsAnnotatedWith(OnClick.class);
            for (Element element : onClickSet) {
                // 转成原始属性元素（结构体元素）
                ExecutableElement executableElement = (ExecutableElement) element;
                String activityName = getActivityName(executableElement);
                List<ExecutableElement> list = onClickMap.get(activityName);
                if(list==null){
                    list = new ArrayList<>();
                    onClickMap.put(activityName,list);
                }
                list.add(executableElement);
                messager.printMessage(Diagnostic.Kind.NOTE,"executableElement >>> "+executableElement.getSimpleName().toString());
            }
            //----------------------------------造币过程------------------------------------
            // 获取Activity完整的字符串类名（包名 + 类名）  todo 这里暂时只对一个类进行处理
            // 获取"com.kangjj.custom.butterknife.MainActivity "中所有控件属性的集合
            List<VariableElement> cacheElements = bindViewMap.get(activityName);
            List<ExecutableElement> clickElements = onClickMap.get(activityName);
            try{
                // 创建一个新的源文件（Class），并返回一个对象以允许写入它
                JavaFileObject javaFileObject = filer.createSourceFile(activityName+"$ViewBinder");
                // 通过属性标签获取包名标签（任意一个属性标签的父节点都是同一个包名）
                String packageName = getPackageName(cacheElements.get(0));
                //定义Writer对象，开启造币过程
                Writer writer = javaFileObject.openWriter();
                // 类名：MainActivity$ViewBinder，不是com.netease.butterknife.MainActivity$ViewBinder
                // 通过属性元素获取它所属的MainActivity类名，再拼接后结果为：MainActivity$ViewBinder
                String activitySimpleName = cacheElements.get(0).getEnclosingElement().getSimpleName().toString()+"$ViewBinder";
                messager.printMessage(Diagnostic.Kind.NOTE,
                        "activityName >>> " + activityName + " / activitySimpleName >>> " + activitySimpleName);
                // 第一行生成包
                writer.write("package "+packageName+";\n");
                // 第二行生成要导入的接口类（必须手动导入）
                writer.write("import com.kangjj.custom.butterknife.library.ViewBinder;\n");
                writer.write("import com.kangjj.custom.butterknife.library.DebouncingOnClickListener;\n");
                writer.write("import android.view.View;\n");
                // 第三行生成类
                writer.write("public class "+activitySimpleName+" implements ViewBinder<"+activityName+"> {\n");
                // 第四行生成bind方法
                writer.write("public void bind(final "+activityName+" target) {\n");
                // 循环生成MainActivity每个控件属性
                for (VariableElement variableElement : cacheElements) {
                    String fieldName = variableElement.getSimpleName().toString();
                    BindView bindView = variableElement.getAnnotation(BindView.class);
                    int id = bindView.value();
                    writer.write("target."+fieldName+" = target.findViewById("+id+");\n");
                }
                for (ExecutableElement executableElement : clickElements) {
                    OnClick onClick = executableElement.getAnnotation(OnClick.class);
                    int id = onClick.value();
                    String methodName = executableElement.getSimpleName().toString();
                    List<? extends VariableElement> parameters = executableElement.getParameters();
                    writer.write("target.findViewById("+id+").setOnClickListener(new DebouncingOnClickListener() {\n");
                    writer.write("public void doClick(View view) {\n");
                    if(parameters.isEmpty()){
                        writer.write("target."+methodName+"();\n}\n});\n");
                    }else{
                        writer.write("target."+methodName+"(view);\n}\n});\n");
                    }
                }
                // 最后结束标签，造币完成
                writer.write("\n}\n}");
                writer.close();
                messager.printMessage(Diagnostic.Kind.NOTE,"end---------------------------");
                return true;
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 通过属性标签获取类名标签，再通过类名标签获取包名标签
     * @param variableElement
     * @return
     */
    private String getActivityName(VariableElement variableElement) {
        //通过属性标签获取类名标签，再通过类名标签获取包名标签
        String packageName = getPackageName(variableElement);
        TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
        // 完整字符串拼接：com.netease.butterknife + "." + MainActivity
        return packageName+"."+typeElement.getSimpleName().toString();
    }

    /**
     * 通过属性标签获取类名标签，再通过类名标签获取包名标签（通过属性节点，找到父节点，再找到父节点的父节点）
     * @param variableElement
     * @return
     */
    private String getPackageName(VariableElement variableElement){
        //属性的父节点是类节点
        TypeElement typeElement = (TypeElement) variableElement.getEnclosingElement();
        //通过类名标签获取包名标签

        //        也可以通过这种方式获得包名标签
//        PackageElement packageElement = (PackageElement) typeElement.getEnclosingElement();
//        String packageName = packageElement.getQualifiedName().toString();
        String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
        messager.printMessage(Diagnostic.Kind.NOTE,"packageName >>> "+packageName);//todo look log
        return packageName;
    }

    private String getActivityName(ExecutableElement executableElement){
       String packageName = getPackageName(executableElement);
       TypeElement typeElement = (TypeElement) executableElement.getEnclosingElement();
       return packageName+"."+typeElement.getSimpleName().toString();

    }

    private String getPackageName(ExecutableElement executableElement){
        TypeElement typeElement = (TypeElement)executableElement.getEnclosingElement();
        String packageName = elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
        messager.printMessage(Diagnostic.Kind.NOTE,"packageName >>> "+packageName);
        return packageName;
    }
}
