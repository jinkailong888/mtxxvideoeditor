/*****************************************************************************
 * ijksdl_thread.c
 *****************************************************************************
 *
 * Copyright (c) 2013 Bilibili
 * copyright (c) 2013 Zhang Rui <bbcallen@gmail.com>
 *
 * This file is part of ijkPlayer.
 *
 * ijkPlayer is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * ijkPlayer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ijkPlayer; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 */

#include <errno.h>
#include <assert.h>
#include <unistd.h>
#include "ijksdl_inc_internal.h"
#include "ijksdl_thread.h"

#ifdef __ANDROID__

#include "ijksdl/android/ijksdl_android_jni.h"

#endif

#if !defined(__APPLE__)

// using ios implement for autorelease
static void *SDL_RunThread(void *data) {
    SDL_Thread *thread = data;
    ALOGI("SDL_RunThread: [%d] %s\n", (int) gettid(), thread->name);
    //给线程设置名字
    pthread_setname_np(pthread_self(), thread->name);
    //运行
    thread->retval = thread->func(thread->data);
#ifdef __ANDROID__
    //线程运行完后一定要 DetachCurrentThread，否则会异常
    SDL_JNI_DetachThreadEnv();
#endif
    return NULL;
}



/**
 * 对于 pthread_create 函数
 * android/pthread库函数
 *
 * int pthread_create(
 * pthread_t* __pthread_ptr,
 * pthread_attr_t const* __attr,
 * void* (*__start_routine)(void*),
 * void*);
 *
 * @param __pthread_ptr   线程id指针，用于存放创建好的线程的id值
 * @param __attr 线程属性，来源于POSIX线程库。对应linux,  android设置成null即可。作为gcc标准方法使用时，它可以传递很多参数，比如:PTHREAD_CREATE_JOINABLE,PTHREAD_CREATE_DETACH等
 * @param __start_routine 函数指针，用于指向子线程的操作函数
 * @param                 参数指针，用于传递参数
 * @return
 */
SDL_Thread *
SDL_CreateThreadEx(SDL_Thread *thread, int (*fn)(void *), void *data, const char *name) {
    thread->func = fn;
    thread->data = data;
    strlcpy(thread->name, name, sizeof(thread->name) - 1);
    //创建线程并开始运行
    int retval = pthread_create(&thread->id, NULL, SDL_RunThread, thread);
    if (retval)
        return NULL;

    return thread;
}

#endif

int SDL_SetThreadPriority(SDL_ThreadPriority priority) {
    struct sched_param sched;
    int policy;
    pthread_t thread = pthread_self();

    if (pthread_getschedparam(thread, &policy, &sched) < 0) {
        ALOGE("pthread_getschedparam() failed");
        return -1;
    }
    if (priority == SDL_THREAD_PRIORITY_LOW) {
        sched.sched_priority = sched_get_priority_min(policy);
    } else if (priority == SDL_THREAD_PRIORITY_HIGH) {
        sched.sched_priority = sched_get_priority_max(policy);
    } else {
        int min_priority = sched_get_priority_min(policy);
        int max_priority = sched_get_priority_max(policy);
        sched.sched_priority = (min_priority + (max_priority - min_priority) / 2);
    }
    if (pthread_setschedparam(thread, policy, &sched) < 0) {
        ALOGE("pthread_setschedparam() failed");
        return -1;
    }
    return 0;
}

void SDL_WaitThread(SDL_Thread *thread, int *status) {
    assert(thread);
    if (!thread)
        return;

    pthread_join(thread->id, NULL);

    if (status)
        *status = thread->retval;
}

void SDL_DetachThread(SDL_Thread *thread) {
    assert(thread);
    if (!thread)
        return;

    pthread_detach(thread->id);
}
