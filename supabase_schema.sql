-- ====================================================================
-- DOORBLY PRODUCTION SUPABASE DATABASE SCHEMA
-- Doorstep Services, Home Maintenance, Cleaning, Appliance, Freelance,
-- Personal, Professional & Corporate Services Customer Platform
-- ====================================================================

-- 1. Enable UUID Extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Customer Profiles Table
CREATE TABLE IF NOT EXISTS public.customer_profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    full_name TEXT NOT NULL,
    phone_number TEXT,
    default_address TEXT,
    default_lat DOUBLE PRECISION,
    default_lng DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Service Categories Table
CREATE TABLE IF NOT EXISTS public.service_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL UNIQUE,
    slug TEXT NOT NULL UNIQUE,
    description TEXT,
    icon_name TEXT DEFAULT 'build',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 4. Services Table
CREATE TABLE IF NOT EXISTS public.services (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    category_id UUID REFERENCES public.service_categories(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    price NUMERIC(10, 2) NOT NULL,
    duration_minutes INTEGER DEFAULT 60,
    is_available BOOLEAN DEFAULT TRUE,
    image_url TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 5. Bookings Table
CREATE TABLE IF NOT EXISTS public.bookings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    customer_name TEXT NOT NULL,
    customer_email TEXT,
    customer_phone TEXT,
    service_id UUID REFERENCES public.services(id) ON DELETE RESTRICT,
    service_title TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'Pending' CHECK (status IN ('Pending', 'Accepted', 'Partner Assigned', 'On The Way', 'Started', 'Completed', 'Cancelled')),
    scheduled_at TIMESTAMPTZ NOT NULL,
    address TEXT NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    additional_instructions TEXT,
    partner_name TEXT,
    partner_phone TEXT,
    total_amount NUMERIC(10, 2) NOT NULL,
    payment_status TEXT NOT NULL DEFAULT 'Pending' CHECK (payment_status IN ('Pending', 'Paid', 'Failed')),
    payment_method TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. Payments Table (Recorded AFTER service completion)
CREATE TABLE IF NOT EXISTS public.payments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id UUID REFERENCES public.bookings(id) ON DELETE CASCADE,
    customer_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    amount NUMERIC(10, 2) NOT NULL,
    payment_method TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'Paid' CHECK (status IN ('Pending', 'Paid', 'Failed')),
    transaction_ref TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 7. Row Level Security (RLS)
ALTER TABLE public.customer_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.service_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.services ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.bookings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payments ENABLE ROW LEVEL SECURITY;

-- Service Categories: Publicly readable by all authenticated and anonymous clients
CREATE POLICY "Allow public read access to service_categories" 
ON public.service_categories FOR SELECT USING (true);

-- Services: Publicly readable by all authenticated and anonymous clients
CREATE POLICY "Allow public read access to services" 
ON public.services FOR SELECT USING (true);

-- Customer Profiles: Customers can read and update only their own profile
CREATE POLICY "Customers can view own profile" 
ON public.customer_profiles FOR SELECT 
USING (auth.uid() = id);

CREATE POLICY "Customers can insert own profile" 
ON public.customer_profiles FOR INSERT 
WITH CHECK (auth.uid() = id);

CREATE POLICY "Customers can update own profile" 
ON public.customer_profiles FOR UPDATE 
USING (auth.uid() = id);

-- Bookings: Customers can read, insert, and update their own bookings
CREATE POLICY "Customers can view own bookings" 
ON public.bookings FOR SELECT 
USING (auth.uid() = customer_id);

CREATE POLICY "Customers can create own bookings" 
ON public.bookings FOR INSERT 
WITH CHECK (auth.uid() = customer_id);

CREATE POLICY "Customers can update own bookings" 
ON public.bookings FOR UPDATE 
USING (auth.uid() = customer_id);

-- Payments: Customers can view and record payments for their own bookings
CREATE POLICY "Customers can view own payments" 
ON public.payments FOR SELECT 
USING (auth.uid() = customer_id);

CREATE POLICY "Customers can insert own payments" 
ON public.payments FOR INSERT 
WITH CHECK (auth.uid() = customer_id);

-- 8. Enable Supabase Realtime for Bookings
ALTER PUBLICATION supabase_realtime ADD TABLE public.bookings;

-- 9. Automatic Profile Creation on User Signup
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.customer_profiles (id, email, full_name, phone_number)
  VALUES (
    new.id,
    new.email,
    COALESCE(new.raw_user_meta_data->>'full_name', 'Doorbly Customer'),
    new.raw_user_meta_data->>'phone_number'
  )
  ON CONFLICT (id) DO NOTHING;
  RETURN new;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE PROCEDURE public.handle_new_user();

-- ====================================================================
-- 10. REAL PRODUCTION CATEGORIES & SERVICES SEED DATA
-- (Real professional doorstep services with realistic pricing)
-- ====================================================================

INSERT INTO public.service_categories (id, name, slug, description, icon_name) VALUES
('11111111-1111-1111-1111-111111111101', 'Home Repair & Maintenance', 'home-repair', 'Carpentry, electrical fixes, plumbing, masonry, and full handyman services', 'build'),
('11111111-1111-1111-1111-111111111102', 'Cleaning Services', 'cleaning', 'Deep home cleaning, bathroom sanitization, sofa cleaning, and kitchen care', 'cleaning_services'),
('11111111-1111-1111-1111-111111111103', 'Appliance Services', 'appliance', 'AC repair, washing machine servicing, refrigerator checkups, and microwave repairs', 'devices'),
('11111111-1111-1111-1111-111111111104', 'Personal & Wellness', 'personal', 'Salon at home, haircut, grooming, spa treatments, and fitness coaching', 'spa'),
('11111111-1111-1111-1111-111111111105', 'Freelance & IT Services', 'freelance-it', 'Home Wi-Fi setup, computer repair, CCTV installation, and smart home setup', 'computer'),
('11111111-1111-1111-1111-111111111106', 'Corporate Services', 'corporate', 'Office sanitization, HVAC quarterly maintenance, and facility repairs', 'business')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.services (id, category_id, title, description, price, duration_minutes, is_available) VALUES
('22222222-2222-2222-2222-222222222101', '11111111-1111-1111-1111-111111111101', 'Doorstep Plumbing Repair', 'Leak detection, tap replacements, pipe repairs, and drain clearing by verified plumbers.', 29.00, 45, true),
('22222222-2222-2222-2222-222222222102', '11111111-1111-1111-1111-111111111101', 'Certified Electrical Inspection & Fixes', 'Switchboard wiring, circuit breaker diagnosis, light installation, and socket repair.', 35.00, 60, true),
('22222222-2222-2222-2222-222222222103', '11111111-1111-1111-1111-111111111101', 'Carpentry & Furniture Assembly', 'Hinge repair, door lock installation, custom furniture assembly, and wooden polish.', 45.00, 90, true),
('22222222-2222-2222-2222-222222222201', '11111111-1111-1111-1111-111111111102', 'Full House Deep Cleaning', 'Comprehensive dusting, floor scrubbing, vacuuming, window wipe-down, and disinfection.', 99.00, 180, true),
('22222222-2222-2222-2222-222222222202', '11111111-1111-1111-1111-111111111102', 'Kitchen & Chimney Deep Cleaning', 'Grease removal, stove scrub, tile descaling, sink disinfection, and exterior cabinetry.', 49.00, 120, true),
('22222222-2222-2222-2222-222222222203', '11111111-1111-1111-1111-111111111102', 'Sofa & Mattress Shampooing', 'Industrial dry-foam shampooing, stain removal, and mite extraction for 3-5 seaters.', 39.00, 75, true),
('22222222-2222-2222-2222-222222222301', '11111111-1111-1111-1111-111111111103', 'Split AC Jet Foam Servicing', 'Coil cleaning, filter flush, blower wash, gas pressure check, and cooling diagnostics.', 39.00, 60, true),
('22222222-2222-2222-2222-222222222302', '11111111-1111-1111-1111-111111111103', 'Washing Machine Diagnostics & Repair', 'Drum inspection, motor check, drain pump unclogging, and vibration calibration.', 32.00, 45, true),
('22222222-2222-2222-2222-222222222303', '11111111-1111-1111-1111-111111111103', 'Refrigerator Cooling Repair', 'Compressor testing, thermostat replacement, defrosting issue fix, and gas top-up.', 42.00, 60, true),
('22222222-2222-2222-2222-222222222401', '11111111-1111-1111-1111-111111111104', 'Executive Haircut & Beard Grooming', 'Professional stylist at doorstep with sanitized tools, styling, and relaxing wash.', 25.00, 45, true),
('22222222-2222-2222-2222-222222222501', '11111111-1111-1111-1111-111111111105', 'Home Wi-Fi Mesh & Network Setup', 'Router configuration, dead-zone elimination, ethernet cabling, and high-speed tuning.', 30.00, 60, true),
('22222222-2222-2222-2222-222222222502', '11111111-1111-1111-1111-111111111105', 'Smart Security CCTV Installation', 'Camera mounting, DVR/NVR pairing, mobile view configuration, and cable management.', 55.00, 90, true),
('22222222-2222-2222-2222-222222222601', '11111111-1111-1111-1111-111111111106', 'Commercial Office Sanitization', 'High-touch surface disinfection, electrostatic spraying, and air quality report.', 120.00, 150, true)
ON CONFLICT (id) DO NOTHING;
