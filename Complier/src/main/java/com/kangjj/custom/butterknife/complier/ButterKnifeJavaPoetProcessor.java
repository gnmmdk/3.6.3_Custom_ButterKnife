package com.kangjj.custom.butterknife.complier;

import com.google.auto.service.AutoService;
import com.kangjj.custom.butterknife.annotation.BindView;
import com.kangjj.custom.butterknife.annotation.OnClick;
import com.kangjj.custom.butterknife.complier.utils.Constants;
import com.kangjj.custom.butterknife.complier.utils.EmptyUtils;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;


/**
 * @Description: todo  make project没反应可以试下Rebuild project
 * @Author: jj.kang
 * @Email: 345498912@qq.com
 * @ProjectName: 3.6.3_Custom_ButterKnife
 * @Package: com.kangjj.custom.butterknife.complier
 * @CreateDate: 2019/12/14 16:43
 */
// 用来生成 META-INF/services/javax.annotation.processing.Processor 文件
@AutoService(Processor.class)
// 允许/支持的注解类型，让注解处理器处理
@SupportedAnnotationTypes({Constants.BINDVIEW_ANNOTATION_TYPES,Constants.ONCLICK_ANNOTATION_TYPES})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ButterKnifeJavaPoetProcessor extends AbstractProcessor {
    // 操作Element工具类 (类、函数、属性都是Element)
    private Elements elementUtils;

    // type(类信息)工具类，包含用于操作TypeMirror的工具方法
    private Types typeUtils;

    // Messager用来报告错误，警告和其他提示信息
    private Messager messager;

    // 文件生成器 类/资源，Filter用来创建新的类文件，class文件以及辅助文件
    private Filer filer;

    private Map<TypeElement, List<VariableElement>> tempBindViewMap = new HashMap<>();
    private Map<TypeElement, List<ExecutableElement>> tempOnClickMap = new HashMap<>();

    // 该方法主要用于一些初始化的操作，通过该方法的参数ProcessingEnvironment可以获取一些列有用的工具类
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        messager.printMessage(Diagnostic.Kind.NOTE,
                "注解处理器初始化完成，开始处理注解------------------------------->");
    }
    
    /**
     * 相当于main函数，开始处理注解
     * 注解处理器的核心方法，处理具体的注解，生成Java文件
     *
     * @param set              使用了支持处理注解的节点集合
     * @param roundEnv 当前或是之前的运行环境,可以通过该对象查找的注解。
     * @return true 表示后续处理器不会再处理（已经处理完成）
     */
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnv) {
        if(!EmptyUtils.isEmpty(set)){
            Set<? extends Element> bindViewElements = roundEnv.getElementsAnnotatedWith(BindView.class);
            Set<? extends Element> onClickElements = roundEnv.getElementsAnnotatedWith(OnClick.class);
            if(!EmptyUtils.isEmpty(bindViewElements) || !EmptyUtils.isEmpty(onClickElements)){
                try{
                    // 赋值临时map存储，用来存放被注解的属性集合
                    valueOfMap(bindViewElements,onClickElements);
                    // 生成类文件，如：
                    createJavaFile();
                    return true;
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private void createJavaFile() throws IOException{
        // 获取ViewBinder接口类型（生成类文件需要实现的接口）
        TypeElement viewBinderType = elementUtils.getTypeElement(Constants.VIEWBINDER);
        TypeElement clickListenerType = elementUtils.getTypeElement(Constants.CLICKLISTENER);
        TypeElement viewType = elementUtils.getTypeElement(Constants.VIEW);
        if(!EmptyUtils.isEmpty(tempBindViewMap)){
            for (Map.Entry<TypeElement, List<VariableElement>> entry : tempBindViewMap.entrySet()) {
                ClassName className = ClassName.get(entry.getKey());
                // 实现接口泛型 implements ViewBinder<MainActivity>
                ParameterizedTypeName typeName = ParameterizedTypeName.get(ClassName.get(viewBinderType),ClassName.get(entry.getKey()));
                // 参数体配置(final MainActivity target)
                ParameterSpec parameterSpec = ParameterSpec.builder(ClassName.get(entry.getKey()),Constants.TARGET_PARAMETER_NAME)
                        .addModifiers(Modifier.FINAL)
                        .build();
                // 方法配置：public void bind(MainActivity target) {
                MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Constants.BIND_METHOD_NAME)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(parameterSpec)
                        .returns(TypeName.VOID);//可不写
                for (VariableElement fieldElement : entry.getValue()) {
                    String fieldName = fieldElement.getSimpleName().toString();
                    int annotationValue = fieldElement.getAnnotation(BindView.class).value();
                    // target.tv = target.findViewById(R.id.tv);
                    String methodContent = "$N.$N = $N.findViewById($L)";//todo 也可以这样写 "$N."+fieldName+" = $N.findViewById($L)" ，下方的fieldName记得去除
                    methodBuilder.addStatement(methodContent,
                            Constants.TARGET_PARAMETER_NAME,
                            fieldName,
                            Constants.TARGET_PARAMETER_NAME,
                            annotationValue);
                }

                if(!EmptyUtils.isEmpty(tempOnClickMap)){
                    for (Map.Entry<TypeElement, List<ExecutableElement>> methodEntry : tempOnClickMap.entrySet()) {
                        if(className.equals(ClassName.get(entry.getKey()))){
                            for (ExecutableElement methodElement : methodEntry.getValue()) {
                                //获取方法名
                                String methodName = methodElement.getSimpleName().toString();
                                // 获取@OnClick注解的值
                                int annotationValue = methodElement.getAnnotation(OnClick.class).value();
                                /**
                                 * target.findViewById(2131165312).setOnClickListener(new DebouncingOnClickListener() {
                                 *      public void doClick(View view) {
                                 *          target.click(view);
                                 *      }
                                 * });
                                 */
                                List<? extends VariableElement> parameters = methodElement.getParameters();
                                String viewParameter = parameters.isEmpty()?"":"view";
                                methodBuilder.beginControlFlow("$N.findViewById($L).setOnClickListener(new $T()",
                                        Constants.TARGET_PARAMETER_NAME,annotationValue,ClassName.get(clickListenerType))
                                        .beginControlFlow("public void doClick($T view)",ClassName.get(viewType))
                                        .addStatement("$N."+methodName+"($N)",Constants.TARGET_PARAMETER_NAME,viewParameter)
                                        .endControlFlow()
                                        .endControlFlow(")")
                                        .build();
                            }
                        }
                    }
                }

                TypeSpec typeSpecBuilder = TypeSpec.classBuilder(className.simpleName()+"$ViewBinder")
                        .addSuperinterface(typeName)//实现ViewBinder接口
                        .addModifiers(Modifier.PUBLIC) // public修饰符
                        .addMethod(methodBuilder.build()) // 方法的构建（方法参数 + 方法体）
                        .build();
                JavaFile.builder(className.packageName(),typeSpecBuilder)
                .build()
                .writeTo(filer);
            }
        }else if(!EmptyUtils.isEmpty(tempOnClickMap)){//如果没有BindView只有OnClick
            for (Map.Entry<TypeElement, List<ExecutableElement>> methodEntry : tempOnClickMap.entrySet()) {
                ClassName className = ClassName.get(methodEntry.getKey());
                // 实现接口泛型 implements ViewBinder<MainActivity>
                ParameterizedTypeName typeName = ParameterizedTypeName.get(ClassName.get(viewBinderType),ClassName.get(methodEntry.getKey()));
                // 参数体配置(final MainActivity target)
                ParameterSpec parameterSpec = ParameterSpec.builder(ClassName.get(methodEntry.getKey()),Constants.TARGET_PARAMETER_NAME)
                        .addModifiers(Modifier.FINAL)
                        .build();
                // 方法配置：public void bind(MainActivity target) {
                MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Constants.BIND_METHOD_NAME)
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .addParameter(parameterSpec)
                        .returns(TypeName.VOID);//可不写

                for (ExecutableElement methodElement : methodEntry.getValue()) {
                    //获取方法名
                    String methodName = methodElement.getSimpleName().toString();
                    // 获取@OnClick注解的值
                    int annotationValue = methodElement.getAnnotation(OnClick.class).value();
                    /**
                     * target.findViewById(2131165312).setOnClickListener(new DebouncingOnClickListener() {
                     *      public void doClick(View view) {
                     *          target.click(view);
                     *      }
                     * });
                     */
                    List<? extends VariableElement> parameters = methodElement.getParameters();
                    String viewParameter = parameters.isEmpty()?"":"view";
                    methodBuilder.beginControlFlow("$N.findViewById($L).setOnClickListener(new $T()",
                            Constants.TARGET_PARAMETER_NAME,annotationValue,ClassName.get(clickListenerType))
                            .beginControlFlow("public void doClick($T view)",ClassName.get(viewType))
                            .addStatement("$N."+methodName+"($N)",Constants.TARGET_PARAMETER_NAME,viewParameter)
                            .endControlFlow()
                            .endControlFlow(")")
                            .build();
                }

                TypeSpec typeSpecBuilder = TypeSpec.classBuilder(className.simpleName()+"$ViewBinder")
                        .addSuperinterface(typeName)//实现ViewBinder接口
                        .addModifiers(Modifier.PUBLIC) // public修饰符
                        .addMethod(methodBuilder.build()) // 方法的构建（方法参数 + 方法体）
                        .build();
                JavaFile.builder(className.packageName(),typeSpecBuilder)
                        .build()
                        .writeTo(filer);
            }
        }


    }

    private void valueOfMap(Set<? extends Element> bindViewElements, Set<? extends Element> onClickElements) {
        if(!EmptyUtils.isEmpty(bindViewElements)){
            for (Element element : bindViewElements) {
                messager.printMessage(Diagnostic.Kind.NOTE, "@BindView >>> " + element.getSimpleName());
                if(element.getKind() == ElementKind.FIELD){
                    VariableElement fieldElement = (VariableElement)element;
                    TypeElement enclosingElement = (TypeElement) fieldElement.getEnclosingElement();
                    // 注解在属性之上，属性节点父节点是类节点
                    if(tempBindViewMap.containsKey(enclosingElement)){
                        // 如果map集合中的key：类节点存在，直接添加属性
                        tempBindViewMap.get(enclosingElement).add(fieldElement);
                    }else{
                        List<VariableElement> fields = new ArrayList<>();
                        fields.add(fieldElement);
                        tempBindViewMap.put(enclosingElement,fields);
                    }
                }
            }
        }

        if(!EmptyUtils.isEmpty(onClickElements)){
            for (Element element : onClickElements) {
                messager.printMessage(Diagnostic.Kind.NOTE, "@OnClick >>> " + element.getSimpleName());
                if(element.getKind() == ElementKind.METHOD){
                    ExecutableElement methodElement = (ExecutableElement) element;
                    TypeElement enclosingElement = (TypeElement)methodElement.getEnclosingElement();
                    if(tempOnClickMap.containsKey(enclosingElement)){
                        tempOnClickMap.get(enclosingElement).add(methodElement);
                    }else{
                        List<ExecutableElement> fields = new ArrayList<>();
                        fields.add(methodElement);
                        tempOnClickMap.put(enclosingElement,fields);
                    }
                }
            }
        }
    }
}
