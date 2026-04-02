package com.randomadjective.prototipodatalayer.base

import androidx.fragment.app.Fragment

abstract class BaseControlFragment(layoutId: Int) : Fragment(layoutId) {

    protected fun sendMessage(message: String) {
        WearMessageSender.sendMessage(requireContext(), message)
    }
}