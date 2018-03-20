/*****************************************************************************
 * ijksdl_mutex.c
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


/**
 *   互斥锁操作封装
 */

#include "ijksdl_mutex.h"
#include <errno.h>
#include <assert.h>
#include <sys/time.h>
#include "ijksdl_inc_internal.h"


/**
 * 创建互斥锁
 * @return
 */
SDL_mutex *SDL_CreateMutex(void)
{
    SDL_mutex *mutex;
    mutex = (SDL_mutex *) mallocz(sizeof(SDL_mutex));
    if (!mutex)
        return NULL;

    if (pthread_mutex_init(&mutex->id, NULL) != 0) {
        free(mutex);
        return NULL;
    }

    return mutex;
}

/**
 * 销毁互斥锁
 * @param mutex
 */
void SDL_DestroyMutex(SDL_mutex *mutex)
{
    if (mutex) {
        pthread_mutex_destroy(&mutex->id);
        free(mutex);
    }
}

void SDL_DestroyMutexP(SDL_mutex **mutex)
{
    if (mutex) {
        SDL_DestroyMutex(*mutex);
        *mutex = NULL;
    }
}

/**
 * 加锁
 * @param mutex
 * @return
 */
int SDL_LockMutex(SDL_mutex *mutex)
{
    assert(mutex);
    if (!mutex)
        return -1;

    return pthread_mutex_lock(&mutex->id);
}

/**
 * 解锁
 * @param mutex
 * @return
 */
int SDL_UnlockMutex(SDL_mutex *mutex)
{
    assert(mutex);
    if (!mutex)
        return -1;

    return pthread_mutex_unlock(&mutex->id);
}


/**
 * pthread_cond_init 函数按参数attr指定的属性创建一个条件变量
   并将条件变量ID 赋值给参数cond，创建失败则返回错误代码
   当需要重新初始化或释放一个条件变量时，应用程序必须保证这个条件变量未被使用
 * @return
 */
SDL_cond *SDL_CreateCond(void)
{
    SDL_cond *cond;
    cond = (SDL_cond *) mallocz(sizeof(SDL_cond));
    if (!cond)
        return NULL;

    if (pthread_cond_init(&cond->id, NULL) != 0) {
        free(cond);
        return NULL;
    }

    return cond;
}

/**
 * 销毁条件变量
 * @param cond
 */
void SDL_DestroyCond(SDL_cond *cond)
{
    if (cond) {
        pthread_cond_destroy(&cond->id);
        free(cond);
    }
}

void SDL_DestroyCondP(SDL_cond **cond)
{

    if (cond) {
        SDL_DestroyCond(*cond);
        *cond = NULL;
    }
}

int SDL_CondSignal(SDL_cond *cond)
{
    assert(cond);
    if (!cond)
        return -1;

    return pthread_cond_signal(&cond->id);
}

int SDL_CondBroadcast(SDL_cond *cond)
{
    assert(cond);
    if (!cond)
        return -1;

    return pthread_cond_broadcast(&cond->id);
}

int SDL_CondWaitTimeout(SDL_cond *cond, SDL_mutex *mutex, uint32_t ms)
{
    int retval;
    struct timeval delta;
    struct timespec abstime;

    assert(cond);
    assert(mutex);
    if (!cond || !mutex) {
        return -1;
    }

    gettimeofday(&delta, NULL);

    abstime.tv_sec = delta.tv_sec + (ms / 1000);
    abstime.tv_nsec = (delta.tv_usec + (ms % 1000) * 1000) * 1000;
    if (abstime.tv_nsec > 1000000000) {
        abstime.tv_sec += 1;
        abstime.tv_nsec -= 1000000000;
    }

    while (1) {
        retval = pthread_cond_timedwait(&cond->id, &mutex->id, &abstime);
        if (retval == 0)
            return 0;
        else if (retval == EINTR)
            continue;
        else if (retval == ETIMEDOUT)
            return SDL_MUTEX_TIMEDOUT;
        else
            break;
    }

    return -1;
}

/**
 * 阻塞在条件变量上
 * @param cond 条件变量
 * @param mutex  互斥锁
 * @return 成功返回0；任何其他返回值都表示错误
 */
int SDL_CondWait(SDL_cond *cond, SDL_mutex *mutex)
{
    assert(cond);
    assert(mutex);
    if (!cond || !mutex)
        return -1;

    return pthread_cond_wait(&cond->id, &mutex->id);
}
