package com.miflix.native2.data

import kotlinx.coroutines.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.*
import org.junit.Test

class WarmConstructionRP92Test {
    @Test(timeout=8000) fun ownerRemainsResponsiveDuringConstruction() = runBlocking {
        val worker=Executors.newSingleThreadExecutor {Thread(it,"test-build")}.asCoroutineDispatcher()
        val owner=Executors.newSingleThreadExecutor {Thread(it,"test-owner")}.asCoroutineDispatcher()
        val entered=CountDownLatch(1);val finish=CountDownLatch(1)
        try {
            val engine=WarmConstructionRP92(worker,owner)
            val result=Any();val released=AtomicInteger()
            val job=async(start=CoroutineStart.UNDISPATCHED) {
                engine.run(build={assertEquals("test-build",Thread.currentThread().name);entered.countDown();check(finish.await(4,TimeUnit.SECONDS));result to Unit},
                    valid={true},configure={resource,_ ->assertEquals("test-owner",Thread.currentThread().name);resource},release={released.incrementAndGet()})
            }
            assertTrue(entered.await(3,TimeUnit.SECONDS))
            assertTrue(withTimeout(1000) {withContext(owner) {true}})
            finish.countDown()
            assertSame(result,job.await());assertEquals(0,released.get())
        } finally {finish.countDown();worker.close();owner.close()}
    }
    @Test(timeout=8000) fun cancelledConstructorReleasesExactlyOnceOnOwner() = runBlocking {
        val worker=Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val owner=Executors.newSingleThreadExecutor {Thread(it,"release-owner")}.asCoroutineDispatcher()
        val entered=CountDownLatch(1);val finish=CountDownLatch(1)
        try {
            val engine=WarmConstructionRP92(worker,owner)
            val released=AtomicInteger();val configured=AtomicInteger()
            val job=async(start=CoroutineStart.UNDISPATCHED) {
                engine.run(build={entered.countDown();check(finish.await(4,TimeUnit.SECONDS));Any() to Unit},valid={true},
                    configure={resource,_ ->configured.incrementAndGet();resource},
                    release={assertEquals("release-owner",Thread.currentThread().name);released.incrementAndGet()})
            }
            assertTrue(entered.await(3,TimeUnit.SECONDS))
            job.cancel();finish.countDown();job.join()
            assertEquals(0,configured.get());assertEquals(1,released.get())
        } finally {finish.countDown();worker.close();owner.close()}
    }
    @Test(timeout=8000) fun staleIdentityNeverConfiguresAResource() = runBlocking {
        val worker=Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val owner=Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        try {
            val engine=WarmConstructionRP92(worker,owner)
            val released=AtomicInteger();val configured=AtomicInteger()
            val job=async {
                engine.run(build={Any() to Unit},valid={false},configure={resource,_ ->configured.incrementAndGet();resource},release={released.incrementAndGet()})
            }
            job.join();assertTrue(job.isCancelled)
            assertEquals(0,configured.get());assertEquals(1,released.get())
        } finally {worker.close();owner.close()}
    }
    @Test fun retryBudgetIsBoundedAndHonorsTheExistingSwitch() {
        assertEquals(0,WarmRetryPolicyRP92.limit(0,true))
        assertEquals(1,WarmRetryPolicyRP92.limit(20,false))
        assertEquals(5,WarmRetryPolicyRP92.limit(20,true))
        assertEquals(3,WarmRetryPolicyRP92.limit(3,true))
    }
}
