package cn.boop.necron.commands

import com.github.stivais.commodore.Commodore
import com.odtheking.odin.OdinMod.mc
import net.minecraft.CrashReport
import net.minecraft.ReportedException

val test = Commodore("test") {
    runs {
        mc.execute {
            val report = CrashReport("Manual Test Crash", RuntimeException("This is a test exception triggered by OdinAddon!"))
            throw ReportedException(report)
        }
    }
}