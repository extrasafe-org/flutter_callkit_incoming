import 'package:json_annotation/json_annotation.dart';

part 'notification_params.g.dart';

/// Object config for Notification Android.
@JsonSerializable(explicitToJson: true)
class NotificationParams {
  const NotificationParams({
    this.id,
    this.showNotification,
    this.subtitle,
    this.callbackText,
    this.isShowCallback,
    this.content,
    this.count,
  });

  final int? id;
  final bool? showNotification;
  final String? subtitle;
  final String? callbackText;
  final bool? isShowCallback;
  final String? content;
  final int? count;

  factory NotificationParams.fromJson(Map<String, dynamic> json) => _$NotificationParamsFromJson(json);
  Map<String, dynamic> toJson() => _$NotificationParamsToJson(this);
}
