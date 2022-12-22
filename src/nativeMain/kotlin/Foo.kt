import jni.*
import kotlinx.cinterop.*

fun main(vararg args: String) {
    require(args.size == 5) { "Needs classpath, lib path, bootclasspath + 2 parameters " }
    val f = "-Djava.class.path=${args[0]}"
    println(f)
    val d = "-Djava.library.path=${args[1]}"
    println(d)
    val jvm = cValuesOf<JavaVMVar>()
    try {
        val (env, vmArgs) = memScoped {
            val options = allocArray<JavaVMOption>(1)
            options[0].optionString = f.cstr.ptr
            // options[1].optionString = "--bootclasspath=${args[2]}".cstr.ptr

            val vmArgs = alloc<JavaVMInitArgs>()
            vmArgs.version = JNI_VERSION_10
            vmArgs.nOptions = 1
            vmArgs.options = options

            alloc<JNIEnvVar>().ptr to vmArgs.ptr
        }
        val resultCreateJvm = JNI_CreateJavaVM(jvm, env.reinterpret(), vmArgs)
        require(resultCreateJvm == JNI_OK)
        println("JVM CREATED")

        val jcls = env.findClass("sample/MainKt")
        println("GOT MainKt")
        val jclEntry = env.getStaticMethod(jcls, "cobolEntry", "(Lsample/Linking;)V")

        val optionsClass = env.findClass("sample/Linking")
        val optionsObject = env.newObject(
            optionsClass,
            env.getMethod(optionsClass, "<init>", "(Ljava/lang/String;I)V"),
            {
                l = env.newUtfString("Hello")
            },
            {
                i = 42
            }
        )
        println("CREATE Main.Linking")

        env.callStaticVoidMethod(jcls, jclEntry, {
            l = optionsObject
        })
        println("CALLED cobolentry")

        val changedI = env.callIntMethod(optionsObject, env.getMethod(optionsClass, "getI", "()I"))
        val changedS =
            env.callObjectMethodA(optionsObject, env.getMethod(optionsClass, "getS", "()Ljava/lang/String;"))

        println("$changedI, ${env.pointed.pointed!!.GetStringChars!!(env, changedS, null)!!.toKString()}")
    } finally {
        println("SHUTDOWN JVM")
        memScoped {
            jvm.ptr[0]!!.pointed.pointed!!.DestroyJavaVM!!(jvm.ptr[0])
        }
        println("FINISHED")
    }
}

private fun CPointer<JNIEnvVar>.newUtfString(string: String): jstring {
    return memScoped {
        pointed.pointed!!.NewStringUTF!!(this@newUtfString, string.cstr.ptr)!!
    }
}

private fun CPointer<JNIEnvVar>.callIntMethod(
    jobject: jobject,
    method: jmethodID,
    vararg values: jvalue.() -> Unit
): jint {
    val f = memScoped {
        allocArray<jvalue>(values.size) {
            values[it].invoke(this)
        }
    }
    return pointed.pointed!!.CallIntMethodA!!(this@callIntMethod, jobject, method, f)
}

private fun CPointer<JNIEnvVar>.callObjectMethodA(
    jobject: jobject,
    method: jmethodID,
    vararg values: jvalue.() -> Unit
): jobject {
    val f = memScoped {
        allocArray<jvalue>(values.size) {
            values[it].invoke(this)
        }
    }
    return pointed.pointed!!.CallObjectMethodA!!(this@callObjectMethodA, jobject, method, f)!!
}

private fun CPointer<JNIEnvVar>.callStaticVoidMethod(
    jClass: jclass,
    method: jmethodID,
    vararg values: jvalue.() -> Unit
) {
    val args = memScoped {
        allocArray<jvalue>(values.size) {
            values[it].invoke(this)
        }
    }
    pointed.pointed!!.CallStaticVoidMethodA!!(this@callStaticVoidMethod, jClass, method, args)
}

private fun CPointer<JNIEnvVar>.getField(jobject: jobject, name: String, type: String): jfieldID {
    return memScoped {
        pointed.pointed!!.GetFieldID!!(this@getField, jobject, name.cstr.ptr, type.cstr.ptr)!!
    }
}

private fun CPointer<JNIEnvVar>.getMethod(jClass: jclass, name: String, parameter: String): jmethodID {
    return memScoped {
        pointed.pointed!!.GetMethodID!!(this@getMethod, jClass, name.cstr.ptr, parameter.cstr.ptr)!!
    }
}

private fun CPointer<JNIEnvVar>.getStaticMethod(jClass: jclass, name: String, parameter: String): jmethodID {
    return memScoped {
        pointed.pointed!!.GetStaticMethodID!!(this@getStaticMethod, jClass, name.cstr.ptr, parameter.cstr.ptr)!!
    }
}

private fun CPointer<JNIEnvVar>.newObject(
    jClass: jclass,
    method: jmethodID,
    vararg values: jvalue.() -> Unit
): jobject {
    val args = memScoped {
        allocArray<jvalue>(values.size) {
            values[it].invoke(this)
        }
    }
    return pointed.pointed!!.NewObjectA!!(this@newObject, jClass, method, args)!!
}

private fun CPointer<JNIEnvVar>.findClass(className: String): jclass {
    return memScoped {
        pointed.pointed!!.FindClass!!(this@findClass, className.cstr.ptr)!!
    }
}
