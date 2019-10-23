/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.codeInsight.gradle

import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.junit.Test
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.PlatformUtils
import org.jetbrains.kotlin.idea.run.KotlinGradleRunConfiguration
import org.jetbrains.kotlin.idea.run.KotlinJvmTestClassGradleConfigurationProducer
import org.jetbrains.kotlin.idea.run.KotlinJvmTestMethodGradleConfigurationProducer
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.plugins.gradle.execution.test.runner.TestClassGradleConfigurationProducer
import org.jetbrains.plugins.gradle.execution.test.runner.TestMethodGradleConfigurationProducer
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions

class GradleTestRunConfigurationTest : GradleImportingTestCase() {
    @Test
    @TargetVersions("4.7+")
    fun testPreferredConfigurations() {
        if (!PlatformUtils.isIntelliJ()) {
            return
        }

        val files = importProjectFromTestData()

        runInEdtAndWait {
            runReadAction {
                val javaFile = files.first { it.name == "MyTest.java" }
                val kotlinFile = files.first { it.name == "MyKotlinTest.kt" }

                val javaClassConfiguration = getConfiguration(javaFile, "MyTest")
                javaClassConfiguration.isProducedBy(TestClassGradleConfigurationProducer::class.java)
                assert(javaClassConfiguration.configuration !is KotlinGradleRunConfiguration)

                val javaMethodConfiguration = getConfiguration(javaFile, "testA")
                javaMethodConfiguration.isProducedBy(TestMethodGradleConfigurationProducer::class.java)
                assert(javaMethodConfiguration.configuration !is KotlinGradleRunConfiguration)

                val kotlinClassConfiguration = getConfiguration(kotlinFile, "MyKotlinTest")
                kotlinClassConfiguration.isProducedBy(KotlinJvmTestClassGradleConfigurationProducer::class.java)
                assert(kotlinClassConfiguration.configuration is KotlinGradleRunConfiguration)

                val kotlinFunctionConfiguration = getConfiguration(kotlinFile, "testA")
                kotlinFunctionConfiguration.isProducedBy(KotlinJvmTestMethodGradleConfigurationProducer::class.java)
                assert(kotlinFunctionConfiguration.configuration is KotlinGradleRunConfiguration)
            }
        }
    }

    private fun getConfiguration(file: VirtualFile, pattern: String): ConfigurationFromContext {
        val psiFile = PsiManager.getInstance(myTestFixture.project).findFile(file) ?: error("PsiFile not found for $file")
        val offset = psiFile.text.indexOf(pattern)
        val psiElement = psiFile.findElementAt(offset)
        val location = PsiLocation(psiElement)
        val context = ConfigurationContext.createEmptyContextForLocation(location)
        return context.configurationsFromContext.orEmpty().singleOrNull() ?: error("Configuration not found for pattern $pattern")
    }

    override fun testDataDirName(): String {
        return "testRunConfigurations"
    }
}