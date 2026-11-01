package com.meridian.exports.application;
public interface ExportSnapshot {
 final class TooLarge extends RuntimeException{}
 byte[] generate(String subject);
}
