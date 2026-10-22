package com.miflix.native2.data

import com.miflix.native2.model.MediaSummary

internal data class PeopleSearchRP81(val rows:List<MediaSummary> = emptyList(),val confidence:Int=0)
