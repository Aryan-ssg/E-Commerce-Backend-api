package com.example.Ecommerce.Order;

public enum OrderStatus {
    PENDING{
        @Override
        public boolean canTransitionTo(OrderStatus next){
            return next==PAID || next==CANCELLED;
        }
    },
    PAID{
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return next==PROCESSING || next==CANCELLED;
        }
    },
    PROCESSING{

        @Override
        public boolean canTransitionTo(OrderStatus next) {
        
            return next==SHIPPED;
        }
        
    },
    SHIPPED {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
          
           return next==DELIVERED;
        }
    },
    DELIVERED {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return false;
            
        }
    },
    CANCELLED {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
         
            return false;
        }
    };

    public abstract boolean canTransitionTo(OrderStatus next);
    
}
