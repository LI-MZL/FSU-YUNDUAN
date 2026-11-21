.class public Lcom/fsu/vpnapp/plugin/ForegroundServicePlugin;
.super Lorg/apache/cordova/CordovaPlugin;
.source "ForegroundServicePlugin.java"


# static fields
.field private static final TAG:Ljava/lang/String; = "FSUForegroundPlugin"

.field private static isRunning:Z = false


# direct methods
.method public constructor <init>()V
    .registers 1

    invoke-direct {p0}, Lorg/apache/cordova/CordovaPlugin;-><init>()V

    return-void
.end method


# virtual methods
.method public execute(Ljava/lang/String;Lorg/apache/cordova/CordovaArgs;Lorg/apache/cordova/CallbackContext;)Z
    .registers 10

    .line 25
    const-string v0, "start"

    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_1b

    .line 26
    invoke-direct {p0}, Lcom/fsu/vpnapp/plugin/ForegroundServicePlugin;->startForegroundService()V

    .line 27
    invoke-virtual {p3}, Lorg/apache/cordova/CallbackContext;->success()V

    .line 28
    const/4 v0, 0x1

    return v0

    .line 30
    :cond_1b
    const-string v0, "stop"

    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_35

    .line 31
    invoke-direct {p0}, Lcom/fsu/vpnapp/plugin/ForegroundServicePlugin;->stopForegroundService()V

    .line 32
    invoke-virtual {p3}, Lorg/apache/cordova/CallbackContext;->success()V

    .line 33
    const/4 v0, 0x1

    return v0

    .line 35
    :cond_35
    const-string v0, "isRunning"

    invoke-virtual {p1, v0}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z

    move-result v0

    if-eqz v0, :cond_4e

    .line 36
    new-instance v0, Lorg/apache/cordova/PluginResult;

    sget-object v1, Lorg/apache/cordova/PluginResult$Status;->OK:Lorg/apache/cordova/PluginResult$Status;

    sget-boolean v2, Lcom/fsu/vpnapp/plugin/ForegroundServicePlugin;->isRunning:Z

    invoke-direct {v0, v1, v2}, Lorg/apache/cordova/PluginResult;-><init>(Lorg/apache/cordova/PluginResult$Status;Z)V

    invoke-virtual {p3, v0}, Lorg/apache/cordova/CallbackContext;->sendPluginResult(Lorg/apache/cordova/PluginResult;)V

    .line 37
    const/4 v0, 0x1

    return v0

    .line 40
    :cond_4e
    const/4 v0, 0x0

    return v0
.end method

.method private startForegroundService()V
    .registers 5

    .line 45
    iget-object v0, p0, Lcom/fsu/vpnapp/plugin/ForegroundServicePlugin;->cordova:Lorg/apache/cordova/CordovaInterface;

    invoke-interface {v0}, Lorg/apache/cordova/CordovaInterface;->getActivity()Landroid/app/Activity;

    move-result-object v0

    .line 46
    new-instance v1, Landroid/content/Intent;

    const-class v2, Lcom/fsu/vpnapp/plugin/ForegroundService;

    invoke-direct {v1, v0, v2}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

    .line 47
    invoke-virtual {v0, v1}, Landroid/app/Activity;->startService(Landroid/content/Intent;)Landroid/content/ComponentName;

    .line 48
    const/4 v2, 0x1

    sput-boolean v2, Lcom/fsu/vpnapp/plugin/ForegroundServicePlugin;->isRunning:Z

    .line 49
    return-void
.end method

.method private stopForegroundService()V
    .registers 5

    .line 53
    iget-object v0, p0, Lcom/fsu/vpnapp/plugin/ForegroundServicePlugin;->cordova:Lorg/apache/cordova/CordovaInterface;

    invoke-interface {v0}, Lorg/apache/cordova/CordovaInterface;->getActivity()Landroid/app/Activity;

    move-result-object v0

    .line 54
    new-instance v1, Landroid/content/Intent;

    const-class v2, Lcom/fsu/vpnapp/plugin/ForegroundService;

    invoke-direct {v1, v0, v2}, Landroid/content/Intent;-><init>(Landroid/content/Context;Ljava/lang/Class;)V

    .line 55
    invoke-virtual {v0, v1}, Landroid/app/Activity;->stopService(Landroid/content/Intent;)Z

    .line 56
    const/4 v2, 0x0

    sput-boolean v2, Lcom/fsu/vpnapp/plugin/ForegroundServicePlugin;->isRunning:Z

    .line 57
    return-void
.end method