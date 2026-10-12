package com.miflix.native2.data

import com.miflix.native2.model.StreamChoice
import org.junit.Assert.*
import org.junit.Test

class StreamFiltersTest {
    private fun link(filename: String="",title: String="",bytes: Long=0)=StreamChoice("Torrentio","https://example.test/video",title,filename=filename,videoSize=bytes)
    @Test fun rejectsWrongMovieYearButKeepsUnknownAndSeriesYears() {
        assertFalse(StreamFilters.allowed(link("Dune.1984.1080p.mkv"),"movie","2021",0))
        assertTrue(StreamFilters.allowed(link("Dune.2021.2160p.mkv"),"movie","2021",0))
        assertTrue(StreamFilters.allowed(link(title="Movie stream 1080p"),"movie","2021",0))
        assertTrue(StreamFilters.allowed(link("Show.2024.S03E01.1080p.mkv"),"series","2020",0))
    }
    @Test fun numericTitlesAndRemasterDoNotOverrideReleaseYear() {
        assertEquals(1968,StreamFilters.releaseYear("2001.A.Space.Odyssey.1968.2160p.mkv"))
        assertEquals(2017,StreamFilters.releaseYear("Blade.Runner.2049.2017.1080p.mkv"))
        assertEquals(1982,StreamFilters.releaseYear("Blade.Runner.1982.Remastered.2019.1080p.mkv"))
        assertNull(StreamFilters.releaseYear("Movie.1920x1080.mkv"))
        assertNull(StreamFilters.releaseYear("Movie 2000 MB"))
    }
    @Test fun filenameYearPrecedesDisplayLabelsAndTechnicalMetadata() {
        assertFalse(StreamFilters.allowed(link("Movie.1990.1080p.mkv","Movie (2020)"),"movie","2020",0))
        assertTrue(StreamFilters.allowed(link("Movie.2020.1080p.mkv","Uploaded 2024"),"movie","2020",0))
        assertFalse(StreamFilters.allowed(link(title="Movie (1990)\n1080p"),"movie","2020",0))
    }
    @Test fun titleNumbersWithoutReleaseYearRemainUnknown() {
        assertTrue(StreamFilters.allowed(link("1917.1080p.mkv"),"movie","2019",0,"1917"))
        assertFalse(StreamFilters.allowed(link("1917.2018.1080p.mkv"),"movie","2019",0,"1917"))
        assertTrue(StreamFilters.allowed(link("Blade.Runner.2049.2160p.mkv"),"movie","2017",0,"Blade Runner 2049"))
    }
    @Test fun capIsInclusiveAndRequiresKnownSize() {
        val cap=5_000_000_000L
        assertTrue(StreamFilters.allowed(link(bytes=cap),"movie",null,cap))
        assertFalse(StreamFilters.allowed(link(bytes=cap+1),"movie",null,cap))
        assertFalse(StreamFilters.allowed(link(),"movie",null,cap))
        assertTrue(StreamFilters.allowed(link(),"movie",null,0))
        assertTrue(StreamFilters.allowed(link(title="1080p 4.5 GB"),"movie",null,cap))
        assertFalse(StreamFilters.allowed(link(title="1080p 5 GiB"),"movie",null,cap))
        assertFalse(StreamFilters.allowed(link(title="TB Download 1 GB"),"movie",null,cap))
    }
}
