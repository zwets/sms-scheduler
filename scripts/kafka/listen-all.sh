#!/bin/bash

exec kcat -J -G kcat-sms-scheduler schedule-sms send-sms sms-status
