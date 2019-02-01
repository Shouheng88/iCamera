package me.shouheng.sample

import android.databinding.DataBindingUtil
import android.databinding.ViewDataBinding
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

/**
 * Created on 2019/2/1.
 */
abstract class CommonFragment<T : ViewDataBinding> : Fragment() {

    lateinit var binding : T

    abstract fun getLayoutResId() : Int

    abstract fun doCreateView(savedInstanceState: Bundle?)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (getLayoutResId() <= 0) throw AssertionError("Subclass must provide a valid layout resource id")
        binding = DataBindingUtil.inflate(inflater, getLayoutResId(), container, false)
        val root = binding.root
        doCreateView(savedInstanceState)
        return root
    }
}