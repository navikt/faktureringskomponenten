package no.nav.melosys.featuretoggle

import io.getunleash.*
import java.util.*
import java.util.function.BiPredicate
import kotlin.collections.ArrayList

class LocalUnleash : Unleash {
    private var enableAll = false
    private var disableAll = false
    private val features = mutableMapOf<String, Boolean>()
    private val variants = mutableMapOf<String, Variant>()
    private val excludedFeatures = mutableMapOf<String, Boolean>()

    override fun isEnabled(toggleName: String): Boolean {
        return isEnabled(toggleName, false)
    }

    override fun isEnabled(toggleName: String, defaultSetting: Boolean): Boolean {
        return when {
            enableAll -> excludedFeatures.getOrDefault(toggleName, true)
            disableAll -> excludedFeatures.getOrDefault(toggleName, false)
            else -> features.getOrDefault(toggleName, defaultSetting)
        }
    }

    override fun isEnabled(toggleName: String, context: UnleashContext): Boolean {
        return super.isEnabled(toggleName, context)
    }

    override fun isEnabled(toggleName: String, context: UnleashContext, defaultSetting: Boolean): Boolean {
        return super.isEnabled(toggleName, context, defaultSetting)
    }

    override fun isEnabled(toggleName: String, fallbackAction: BiPredicate<String, UnleashContext>): Boolean {
        return super.isEnabled(toggleName, fallbackAction)
    }

    override fun isEnabled(s: String, unleashContext: UnleashContext, biPredicate: BiPredicate<String, UnleashContext>): Boolean {
        return false // TODO: se om vi faktisk bruker dette
    }

    override fun getVariant(toggleName: String, context: UnleashContext): Variant {
        return getVariant(toggleName, Variant.DISABLED_VARIANT)
    }

    override fun getVariant(toggleName: String, context: UnleashContext, defaultValue: Variant): Variant {
        return getVariant(toggleName, defaultValue)
    }

    override fun getVariant(toggleName: String): Variant {
        return getVariant(toggleName, Variant.DISABLED_VARIANT)
    }

    override fun getVariant(toggleName: String, defaultValue: Variant): Variant {
        return if (isEnabled(toggleName) && variants.containsKey(toggleName)) {
            variants[toggleName]!!
        } else {
            defaultValue
        }
    }

    override fun getFeatureToggleNames(): MutableList<String> {
        TODO("Not yet implemented")
    }

    override fun shutdown() {
        super.shutdown()
    }

    override fun more(): MoreOperations {
        TODO("Not yet implemented")
    }

    fun enableAllExcept(vararg excludedFeatures: String) {
        enableAll()
        excludedFeatures.forEach { this.excludedFeatures[it] = false }
    }

    fun disableAllExcept(vararg excludedFeatures: String) {
        disableAll()
        excludedFeatures.forEach { this.excludedFeatures[it] = true }
    }

    fun enableAll() {
        disableAll = false
        enableAll = true
        features.clear()
        excludedFeatures.clear()
    }

    fun disableAll() {
        disableAll = true
        enableAll = false
        features.clear()
        excludedFeatures.clear()
    }

    fun resetAll() {
        disableAll = false
        enableAll = false
        features.clear()
        variants.clear()
        excludedFeatures.clear()
    }

    fun enable(vararg features: String) {
        features.forEach { this.features[it] = true }
    }

    fun disable(vararg features: String) {
        features.forEach { this.features[it] = false }
    }

    fun reset(vararg features: String) {
        features.forEach { this.features.remove(it) }
    }

    fun setVariant(t1: String, a: Variant) {
        variants[t1] = a
    }
}
