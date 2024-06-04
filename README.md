# TimeSync

WebCTRL is a trademark of Automated Logic Corporation. Any other trademarks mentioned herein are the property of their respective owners.

## About

This WebCTRL add-on [(download link)](https://github.com/automatic-controls/time-sync-addon/releases/latest/download/TimeSync.addon) can be configured with [Cron expressions](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/scheduling/support/CronExpression.html#parse(java.lang.String)) to automatically trigger controller time synchronization at regularly scheduled intervals. If your WebCTRL server requires signed add-ons, download [*ACES.cer*](https://github.com/automatic-controls/addon-dev-script/blob/main/ACES.cer?raw=true) for authentication (place this certificate in the *./addons* directors of your WebCTRL server). This add-on should be compatible with WebCTRL 8.0, 8.5, and 9.0.

WebCTRL servers have built-in functionality to trigger controller time synchronization once daily at a specified time; however, this add-on may be used to satisfy any additional advanced scheduling requirements. The Cron expression may be configured on this add-on's main page (accessible from system settings). For example, `0 0 * * * *` specifies hourly synchronization.

Schedule granularity is 5 minutes. This means the smallest allowed time interval between scheduled runs is 5 minutes. It also means that scheduled runs may occur within &plusmn;5 minutes of the designated time.