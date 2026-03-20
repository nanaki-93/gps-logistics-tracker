package model

import (
	"fmt"
	"time"
)

type GpsEvent struct {
	DriverUid  string    `json:"driverUid"`
	Lat        float64   `json:"lat"`
	Lng        float64   `json:"lng"`
	RecordedAt time.Time `json:"recordedAt"`
}

func (e *GpsEvent) Validate() error {
	if e.DriverUid == "" {
		return fmt.Errorf("driverUid cannot be empty")
	}
	if e.RecordedAt.IsZero() {
		return fmt.Errorf("recordedAt cannot be empty")
	}
	if e.RecordedAt.After(time.Now().Add(20 * time.Second)) {
		return fmt.Errorf("recordedAt cannot be in the future")
	}
	return nil
}
