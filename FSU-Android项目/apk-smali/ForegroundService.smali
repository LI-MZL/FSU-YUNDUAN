.class public Lcom/fsu/vpnapp/plugin/ForegroundService;
.super Landroid/app/Service;
.source "ForegroundService.java"


# static fields
.field private static final CHANNEL_ID:Ljava/lang/String; = "fsu_foreground_channel"

.field private static final NOTIFICATION_ID:I = 0x7f0a0001


# direct methods
.method public constructor <init>()V
    .registers 1

    invoke-direct {p0}, Landroid/app/Service;-><init>()V

    return-void
.end method

.method public onCreate()V
    .registers 4
    .annotation build Landroid/annotation/SuppressLint;
        value = {
            "NewApi"
        }
    .end annotation

    invoke-super {p0}, Landroid/app/Service;->onCreate()V

    .line 30
    sget v0, Landroid/os/Build$VERSION;->SDK_INT:I

    const/16 v1, 0x1a

    if-lt v0, v1, :cond_27

    .line 31
    new-instance v0, Landroid/app/NotificationChannel;

    .line 32
    const-string v1, "fsu_foreground_channel"

    .line 33
    const-string v2, "FSU\u4e91\u5e73\u53f0\u76d1\u6d4b"

    .line 34
    const/4 v3, 0x2

    invoke-direct {v0, v1, v2, v3}, Landroid/app/NotificationChannel;-><init>(Ljava/lang/String;Ljava/lang/CharSequence;I)V

    .line 35
    const-string v1, "\u524d\u53f0\u670d\u52a1\u4fdd\u6301\u5e94\u7528\u5728\u7ebf"

    invoke-virtual {v0, v1}, Landroid/app/NotificationChannel;->setDescription(Ljava/lang/String;)V

    .line 36
    const/4 v1, 0x0

    invoke-virtual {v0, v1}, Landroid/app/NotificationChannel;->setShowBadge(Z)V

    .line 37
    const-string v1, "notification"

    invoke-virtual {p0, v1}, Lcom/fsu/vpnapp/plugin/ForegroundService;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;

    move-result-object v1

    check-cast v1, Landroid/app/NotificationManager;

    .line 38
    invoke-virtual {v1, v0}, Landroid/app/NotificationManager;->createNotificationChannel(Landroid/app/NotificationChannel;)V

    :cond_27
    return-void
.end method

.method public onStartCommand(Landroid/content/Intent;II)I
    .registers 6
    .annotation build Landroid/annotation/SuppressLint;
        value = {
            "NewApi"
        }
    .end annotation

    .line 45
    new-instance v0, Landroid/app/Notification$Builder;

    invoke-direct {v0, p0}, Landroid/app/Notification$Builder;-><init>(Landroid/content/Context;)V

    .line 46
    const-string v1, "fsu_foreground_channel"

    invoke-virtual {v0, v1}, Landroid/app/Notification$Builder;->setChannelId(Ljava/lang/String;)Landroid/app/Notification$Builder;

    move-result-object v0

    .line 47
    const-string v1, "\u4e91\u7aefFSU\u5728\u7ebf\u76d1\u6d4b\u4e2d"

    invoke-virtual {v0, v1}, Landroid/app/Notification$Builder;->setContentTitle(Ljava/lang/CharSequence;)Landroid/app/Notification$Builder;

    move-result-object v0

    .line 48
    const-string v1, "\u5e94\u7528\u6b63\u5728\u540e\u53f0\u8fd0\u884c\uff0c\u4fdd\u6301FSU\u8bbe\u5907\u5728\u7ebf"

    invoke-virtual {v0, v1}, Landroid/app/Notification$Builder;->setContentText(Ljava/lang/CharSequence;)Landroid/app/Notification$Builder;

    move-result-object v0

    .line 49
    const v1, 0x1080055

    invoke-virtual {v0, v1}, Landroid/app/Notification$Builder;->setSmallIcon(I)Landroid/app/Notification$Builder;

    move-result-object v0

    .line 50
    const/4 v1, 0x1

    invoke-virtual {v0, v1}, Landroid/app/Notification$Builder;->setOngoing(Z)Landroid/app/Notification$Builder;

    move-result-object v0

    .line 51
    const/4 v2, 0x2

    invoke-virtual {v0, v2}, Landroid/app/Notification$Builder;->setPriority(I)Landroid/app/Notification$Builder;

    move-result-object v0

    .line 52
    invoke-virtual {v0}, Landroid/app/Notification$Builder;->build()Landroid/app/Notification;

    move-result-object v0

    .line 53
    const v2, 0x7f0a0001

    invoke-virtual {p0, v2, v0}, Lcom/fsu/vpnapp/plugin/ForegroundService;->startForeground(ILandroid/app/Notification;)V

    .line 55
    return v1
.end method

.method public onDestroy()V
    .registers 2

    .line 59
    const/4 v0, 0x1

    invoke-virtual {p0, v0}, Lcom/fsu/vpnapp/plugin/ForegroundService;->stopForeground(Z)V

    .line 60
    invoke-super {p0}, Landroid/app/Service;->onDestroy()V

    return-void
.end method

.method public onBind(Landroid/content/Intent;)Landroid/os/IBinder;
    .registers 3

    .line 64
    const/4 v0, 0x0

    return-object v0
.end method