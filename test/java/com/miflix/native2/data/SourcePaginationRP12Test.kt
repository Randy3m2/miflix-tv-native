package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import org.junit.Assert.*
import org.junit.Test

class SourcePaginationRP12Test {
 private val rows=(1..20).flatMap {i ->listOf(
  StreamChoice("a$i","https://test/a$i","Latino",addonName="A"),
  StreamChoice("b$i","https://test/b$i","English",addonName="B"))}
 @Test fun sevenInitiallyPerAddonAndLoadMoreRevealsNextSevenWithoutDroppingCache() {
  val initial=SourcePaginationRP12.visible(rows,emptyMap(),emptyMap())
  assertEquals(14,initial.size)
  assertEquals(7,initial.count {it.addonName=="A"})
  val second=SourcePaginationRP12.visible(rows,emptyMap(),mapOf("A" to 2,"B" to 2))
  assertEquals(28,second.size)
  assertEquals(14,second.count {it.addonName=="B"})
  assertEquals(rows,SourcePaginationRP12.visible(rows,emptyMap(),mapOf("A" to 3,"B" to 3)))
  assertEquals(40,rows.size)
 }
 @Test fun perAddonSizeOverridesAndAllAreRespected() {
  val result=SourcePaginationRP12.visible(rows,mapOf("A" to 5,"B" to 0),emptyMap())
  assertEquals(5,result.count {it.addonName=="A"})
  assertEquals(20,result.count {it.addonName=="B"})
 }
 @Test fun currentPlayingSourceRemainsVisibleBeyondFirstBatch() {
  val result=SourcePaginationRP12.visible(rows,emptyMap(),emptyMap(),"https://test/a19")
  assertTrue(result.any {it.url=="https://test/a19"})
  assertEquals(7,result.count {it.addonName=="B"})
 }
}
