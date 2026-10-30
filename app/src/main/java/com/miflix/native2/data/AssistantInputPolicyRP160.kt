package com.miflix.native2.data

internal object AssistantInputPolicyRP160 {
    /** Shared keyboard state can arrive one frame before the assistant's query mirror. */
    fun discardPending(pending:String?,current:String,shared:String?):Boolean =
        pending!=null && pending!=current && (shared==null || shared==current)
}
