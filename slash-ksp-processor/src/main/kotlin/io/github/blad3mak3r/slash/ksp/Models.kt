package io.github.blad3mak3r.slash.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration

/**
 * Collected data about a single class annotated with @ApplicationCommand,
 * gathered during the KSP resolve phase.
 */
data class CommandModel(
    val classDecl: KSClassDeclaration,
    val commandName: String,
    val commandType: String,              // "SLASH" | "MESSAGE" | "USER"
    val classRequire: List<String>,       // fully-qualified precondition class names from @Require on the class
    val classPermissions: PermissionsModel?,
    val classRateLimit: RateLimitModel?,
    val slashHandlers: List<SlashHandlerModel>,
    val autoCompleteHandlers: List<AutoCompleteHandlerModel>,
    val buttonHandlers: List<ButtonHandlerModel>,
    val modalHandlers: List<ModalHandlerModel>,
    val userHandler: UserHandlerModel?,
    val messageHandler: MessageHandlerModel?
)

data class SlashHandlerModel(
    val function: KSFunctionDeclaration,
    val group: String,
    val name: String,
    val target: String,                   // "GUILD" | "DM" | "ALL"
    val supportDetached: Boolean,
    val require: List<String>,
    val permissions: PermissionsModel?,
    val rateLimit: RateLimitModel?,
    val parameters: List<ParameterModel>
)

data class AutoCompleteHandlerModel(
    val function: KSFunctionDeclaration,
    val group: String,
    val name: String,
    val optionName: String,
    val parameters: List<ParameterModel>  // extra params after ctx, resolved as Discord options
)

data class ButtonHandlerModel(
    val function: KSFunctionDeclaration,
    val pattern: String
)

data class ModalHandlerModel(
    val function: KSFunctionDeclaration,
    val pattern: String
)

data class UserHandlerModel(
    val function: KSFunctionDeclaration,
    val require: List<String>
)

data class MessageHandlerModel(
    val function: KSFunctionDeclaration,
    val require: List<String>
)

data class ParameterModel(
    val optionName: String,
    val kotlinType: String,   // e.g. "String", "Long", "Boolean", "Member", etc.
    val nullable: Boolean
)

data class PermissionsModel(
    val permissions: List<String>,   // e.g. ["MESSAGE_SEND", "KICK_MEMBERS"]
    val target: String               // "USER" | "BOT"
)

data class RateLimitModel(
    val limit: Int,
    val period: Long
)
