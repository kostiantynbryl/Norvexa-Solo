# Norvexa Flow release rules.
# Room and Hilt provide their own consumer rules.

# Keep database model field names stable for support diagnostics and migrations.
-keepclassmembers class com.norvexa.flow.data.local.entity.** { *; }
